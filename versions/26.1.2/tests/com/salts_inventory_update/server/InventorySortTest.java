package com.salts_inventory_update.server;

import com.salts_inventory_update.internal.desktop.InventorySortPlan;
import com.salts_inventory_update.internal.desktop.SmartInventoryOrder;
import com.salts_inventory_update.network.DesktopPackets.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InventorySortTest {
    private static sun.misc.Unsafe unsafe;
    private ServerPlayer player;
    private Object sessions;
    private final Comparator<ItemStack> order = (a, b) -> new SmartInventoryOrder().comparator().compare(a, b);

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        for (Item item : List.of(Items.DIRT, Items.GRAVEL, Items.SAND, Items.COBBLESTONE, Items.DIAMOND,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.OAK_LOG, Items.OAK_PLANKS, Items.SPRUCE_LOG,
            Items.SPRUCE_PLANKS, Items.APPLE, Items.BONE, Items.DIAMOND_SWORD, Items.IRON_SWORD)) {
            var components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64);
            if (item == Items.APPLE) components.set(DataComponents.FOOD, new net.minecraft.world.food.FoodProperties(4, 0.3F, false));
            if (item == Items.DIAMOND_SWORD || item == Items.IRON_SWORD) components
                .set(DataComponents.MAX_STACK_SIZE, 1).set(DataComponents.DAMAGE, 0)
                .set(DataComponents.MAX_DAMAGE, item == Items.DIAMOND_SWORD ? 1561 : 250)
                .set(DataComponents.WEAPON, new net.minecraft.world.item.component.Weapon(1));
            item.builtInRegistryHolder().bindComponents(components.build());
        }
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }

    @BeforeEach void setup() throws Exception {
        player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        var inventory = (InventoryMenu) unsafe.allocateInstance(InventoryMenu.class); inventory.setCarried(ItemStack.EMPTY);
        Field f = Player.class.getDeclaredField("inventoryMenu"); f.setAccessible(true); f.set(player, inventory);
        sessions = SortWindowsTest.construct("PlayerSessions");
    }

    private static SortWindowsTest.TestMenu menu(int size) { return new SortWindowsTest.TestMenu(size); }
    private List<ItemStack> plan(SortWindowsTest.TestMenu menu) {
        return InventorySortPlan.create(menu.slots, new SmartInventoryOrder().comparator()).orElseThrow();
    }
    private boolean execute(SortWindowsTest.TestMenu menu, List<Slot> slots) throws Throwable {
        Method method = Arrays.stream(DesktopContainerSessions.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("executeInventorySort")).findFirst().orElseThrow();
        method.setAccessible(true);
        try { return (boolean) method.invoke(null, player, sessions, menu, slots, new SmartInventoryOrder().comparator()); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }

    @Test void categoriesPutResourcesFirstAndBulkTerrainLast() throws Exception {
        withTags(Items.DIAMOND, List.of("c:gems", "c:gems/diamond"), () -> {
            var menu = menu(9).put(0, Items.DIRT, 3).put(1, Items.OAK_PLANKS, 4).put(2, Items.BONE, 5)
                .put(3, Items.APPLE, 6).put(4, Items.DIAMOND_SWORD, 1).put(5, Items.IRON_INGOT, 7).put(6, Items.DIAMOND, 8);
            var sorted = plan(menu);
            assertEquals(List.of(Items.DIAMOND, Items.IRON_INGOT, Items.DIAMOND_SWORD, Items.APPLE, Items.BONE, Items.OAK_PLANKS, Items.DIRT),
                sorted.stream().filter(s -> !s.isEmpty()).map(ItemStack::getItem).toList());
        });
    }

    @Test void commonTagsRecognizeAnItemWithoutAnIngotIdentifier() throws Exception {
        withTags(Items.BONE, List.of("c:ingots", "c:ingots/test_metal"), () ->
            assertTrue(order.compare(new ItemStack(Items.BONE), new ItemStack(Items.APPLE)) < 0));
    }

    @Test void datapackCategoryOverrideTakesPrecedence() throws Exception {
        withTags(Items.DIRT, List.of("salts_inventory_update:sort/valuables"), () ->
            assertTrue(order.compare(new ItemStack(Items.DIRT), new ItemStack(Items.APPLE)) < 0));
    }

    @Test void materialFamiliesStayTogether() {
        var m = menu(4).put(0, Items.SPRUCE_PLANKS, 1).put(1, Items.OAK_LOG, 1)
            .put(2, Items.SPRUCE_LOG, 1).put(3, Items.OAK_PLANKS, 1);
        var sorted = plan(m);
        assertTrue(sorted.subList(0, 2).stream().allMatch(s -> s.is(Items.OAK_LOG) || s.is(Items.OAK_PLANKS)));
        assertTrue(sorted.subList(2, 4).stream().allMatch(s -> s.is(Items.SPRUCE_LOG) || s.is(Items.SPRUCE_PLANKS)));
    }

    @Test void mergesStacksAndPreservesCustomComponents() {
        var m = menu(5).put(0, Items.IRON_INGOT, 40).put(1, Items.IRON_INGOT, 40).put(2, Items.IRON_INGOT, 5);
        m.slots.get(2).getItem().set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keepsake"));
        var sorted = plan(m);
        assertEquals(85, sorted.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(List.of(64, 16, 5), sorted.stream().filter(s -> !s.isEmpty()).map(ItemStack::getCount).toList());
        assertEquals("Keepsake", sorted.get(2).getCustomName().getString());
        assertEquals(40, m.slots.get(0).getItem().getCount(), "Planning must not mutate live stacks");
    }

    @Test void damagedEquipmentRemainsDistinctAndBetterConditionComesFirst() {
        var m = menu(3).put(0, Items.DIAMOND_SWORD, 1).put(1, Items.IRON_SWORD, 1).put(2, Items.DIAMOND_SWORD, 1);
        m.slots.get(0).getItem().set(DataComponents.DAMAGE, 100);
        var sorted = plan(m);
        assertTrue(sorted.get(0).is(Items.DIAMOND_SWORD)); assertEquals(0, sorted.get(0).getDamageValue());
        assertEquals(100, sorted.get(1).getDamageValue()); assertTrue(sorted.get(2).is(Items.IRON_SWORD));
    }

    @Test void obeysPerSlotStackLimits() {
        var m = menu(3).put(0, Items.IRON_INGOT, 28);
        for (int i = 0; i < 3; i++) { final int index = i;
            m.slots.set(i, new Slot(m.storage, index, 0, 0) { @Override public int getMaxStackSize(ItemStack stack) { return 16; } });
        }
        assertEquals(List.of(16, 12, 0), plan(m).stream().map(ItemStack::getCount).toList());
    }

    @Test void impossibleRestrictedLayoutIsRefusedWithoutChangingContents() {
        var m = menu(2).put(0, Items.IRON_INGOT, 64).put(1, Items.IRON_INGOT, 64);
        m.slots.set(1, new Slot(m.storage, 1, 0, 0) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        assertTrue(InventorySortPlan.create(m.slots, order).isEmpty()); assertEquals(128, m.total());
    }

    @Test void fullInventorySortIsIdempotentAndConservesEveryStack() throws Throwable {
        var m = menu(4).put(0, Items.DIRT, 64).put(1, Items.IRON_INGOT, 40).put(2, Items.GRAVEL, 3).put(3, Items.IRON_INGOT, 40);
        assertTrue(execute(m, m.slots));
        var first = m.slots.stream().map(s -> s.getItem().copy()).toList();
        assertTrue(execute(m, m.slots));
        for (int i = 0; i < first.size(); i++) assertTrue(ItemStack.matches(first.get(i), m.slots.get(i).getItem()));
        assertEquals(147, m.total()); assertEquals(80, m.count(Items.IRON_INGOT));
    }

    @Test void protectedSlotsAreNotRearranged() throws Throwable {
        var m = menu(3).put(0, Items.DIRT, 10).put(1, Items.IRON_INGOT, 5).put(2, Items.BONE, 9);
        assertTrue(execute(m, List.of(m.slots.get(0), m.slots.get(1))));
        assertTrue(m.slots.get(2).getItem().is(Items.BONE)); assertEquals(9, m.slots.get(2).getItem().getCount());
    }

    @Test void callbackChangingProtectedNeighbourRollsBack() {
        var m = menu(3).put(0, Items.DIRT, 10).put(1, Items.IRON_INGOT, 5).put(2, Items.BONE, 9);
        boolean[] fired = {false};
        m.slots.set(0, new Slot(m.storage, 0, 0, 0) {
            @Override public void setByPlayer(ItemStack stack, ItemStack before) {
                super.setByPlayer(stack, before);
                if (!fired[0]) { fired[0] = true; m.put(2, Items.DIRT, 1); }
            }
        });
        assertThrows(IllegalStateException.class, () -> execute(m, List.of(m.slots.get(0), m.slots.get(1))));
        assertEquals(10, m.count(Items.DIRT)); assertEquals(5, m.count(Items.IRON_INGOT)); assertEquals(9, m.count(Items.BONE));
    }

    @Test void nonemptyCursorDoesNotSort() throws Throwable {
        var m = menu(2).put(0, Items.DIRT, 10).put(1, Items.IRON_INGOT, 5);
        player.inventoryMenu.setCarried(new ItemStack(Items.APPLE));
        assertFalse(execute(m, m.slots)); assertTrue(m.slots.get(0).getItem().is(Items.DIRT));
    }

    @Test void malformedPayloadIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopSortInventoryPayload(1, 0, 1, new DesktopSessionReference(0, 1, 0)));
        assertThrows(IllegalArgumentException.class, () -> new DesktopSortInventoryPayload(1, 1, 0, new DesktopSessionReference(0, 1, 0)));
        assertThrows(IllegalArgumentException.class, () -> new DesktopSortInventoryPayload(1, 1, 1, null));
    }

    private static void withTags(Item item, List<String> tags, Runnable check) throws Exception {
        var holder = item.builtInRegistryHolder(); var original = holder.tags().toList();
        Method bind = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class); bind.setAccessible(true);
        try {
            bind.invoke(holder, tags.stream().map(id -> TagKey.create(Registries.ITEM, Identifier.parse(id))).toList());
            check.run();
        } finally { bind.invoke(holder, original); }
    }
}
