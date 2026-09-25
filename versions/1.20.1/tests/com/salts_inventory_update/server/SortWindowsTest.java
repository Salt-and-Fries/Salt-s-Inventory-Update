package com.salts_inventory_update.server;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import com.salts_inventory_update.internal.desktop.DesktopSortSlots;
import com.salts_inventory_update.network.DesktopPackets.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SortWindowsTest {
    private static sun.misc.Unsafe unsafe;
    private ServerPlayer player;
    private Object sessions;
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }
    @BeforeEach void setup() throws Exception {
        player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);
        InventoryMenu inventory = (InventoryMenu) unsafe.allocateInstance(InventoryMenu.class);
        inventory.setCarried(ItemStack.EMPTY);
        Field field = Player.class.getDeclaredField("inventoryMenu"); field.setAccessible(true); field.set(player, inventory);
        sessions = construct("PlayerSessions");
    }
    static Object construct(String name, Object... args) throws Exception {
        Class<?> type = Class.forName(DesktopContainerSessions.class.getName() + "$" + name);
        for (Constructor<?> ctor : type.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == args.length) { ctor.setAccessible(true); return ctor.newInstance(args); }
        }
        throw new AssertionError(name);
    }
    void sort(TestMenu source, List<TestMenu> targets, boolean shift, int focus) throws Exception {
        List<Object> sources = new ArrayList<>(); List<ItemStack> snapshots = new ArrayList<>();
        for (Slot slot : source.slots) if (slot.hasItem()) {
            sources.add(construct("SlotSource", 0, source, slot, null)); snapshots.add(slot.getItem().copy());
        }
        List<Object> destinations = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            TestMenu target = targets.get(i);
            Object authorization = construct("SessionAuthorization", i + 1, target, null, "");
            destinations.add(construct("SortDestination", authorization, List.copyOf(target.slots),
                target.slots.stream().map(slot -> slot.getItem().copy()).toList()));
        }
        Method method = Arrays.stream(DesktopContainerSessions.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("executeSortTransfers")).findFirst().orElseThrow();
        method.setAccessible(true);
        try { method.invoke(null, player, sessions, sources, snapshots, destinations, focus, shift); }
        catch (InvocationTargetException e) { throw new AssertionError(e.getCause()); }
    }
    @Test void fiveWindowExample() throws Exception {
        TestMenu source = new TestMenu(5).put(0, Items.BONE, 5).put(1, Items.IRON_INGOT, 6)
            .put(2, Items.SAND, 7).put(3, Items.GRAVEL, 8).put(4, Items.GOLD_INGOT, 9);
        TestMenu first = new TestMenu(4).put(0, Items.DIRT, 1).put(1, Items.SAND, 1);
        TestMenu second = new TestMenu(4).put(0, Items.IRON_INGOT, 1).put(1, Items.DIAMOND, 1);
        TestMenu third = new TestMenu(4).put(0, Items.ROTTEN_FLESH, 1).put(1, Items.BONE, 1);
        TestMenu empty = new TestMenu(4);
        sort(source, List.of(first, second, third, empty), false, -1);
        assertEquals(6, third.count(Items.BONE)); assertEquals(7, second.count(Items.IRON_INGOT));
        assertEquals(8, first.count(Items.SAND)); assertEquals(8, source.count(Items.GRAVEL));
        assertEquals(9, source.count(Items.GOLD_INGOT)); assertEquals(0, empty.total()); assertEquals(17, source.total());
    }
    @Test void focusedBeforeEmptyAndPartialCapacity() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.GRAVEL, 12);
        TestMenu empty = new TestMenu(2);
        TestMenu focus = new TestMenu(1).put(0, Items.GRAVEL, 60);
        sort(source, List.of(empty, focus), true, 2);
        assertEquals(64, focus.total()); assertEquals(8, empty.total()); assertEquals(0, source.total());
        source.put(0, Items.GOLD_INGOT, 3);
        TestMenu populatedFocus = new TestMenu(2).put(0, Items.DIRT, 1);
        TestMenu anotherEmpty = new TestMenu(1);
        sort(source, List.of(anotherEmpty, populatedFocus), true, 2);
        assertEquals(3, populatedFocus.count(Items.GOLD_INGOT)); assertEquals(0, anotherEmpty.total());
    }
    @Test void fullExactMatchesContinueToTagsThenFallback() throws Exception {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("sort_test", "metals"));
        Method bind = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class); bind.setAccessible(true);
        var iron = Items.IRON_INGOT.builtInRegistryHolder(); var gold = Items.GOLD_INGOT.builtInRegistryHolder();
        var ironTags = iron.tags().toList(); var goldTags = gold.tags().toList();
        try {
            bind.invoke(iron, List.of(tag)); bind.invoke(gold, List.of(tag));
            TestMenu source = new TestMenu(1).put(0, Items.IRON_INGOT, 12);
            TestMenu exact = new TestMenu(1).put(0, Items.IRON_INGOT, 64);
            TestMenu tagged = new TestMenu(2).put(0, Items.GOLD_INGOT, 1);
            TestMenu focused = new TestMenu(2);
            sort(source, List.of(exact, tagged, focused), false, 3);
            assertEquals(12, source.total()); assertEquals(1, tagged.total());
            sort(source, List.of(exact, tagged, focused), true, 3);
            assertEquals(12, tagged.count(Items.IRON_INGOT)); assertEquals(0, focused.total());
        } finally { bind.invoke(iron, ironTags); bind.invoke(gold, goldTags); }
    }
    @Test void exactBeatsFocusedEmptyRegardlessOfOpeningOrder() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.DIRT, 10);
        TestMenu empty = new TestMenu(1);
        TestMenu exact = new TestMenu(1).put(0, Items.DIRT, 1);
        sort(source, List.of(empty, exact), true, 1);
        assertEquals(11, exact.total()); assertEquals(0, empty.total());
    }
    @Test void noDestinationOrFullDestinationPreservesItems() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.DIRT, 10);
        sort(source, List.of(), true, -1); assertEquals(10, source.total());
        TestMenu full = new TestMenu(1).put(0, Items.DIRT, 64);
        sort(source, List.of(full), true, 1); assertEquals(10, source.total()); assertEquals(64, full.total());
    }
    @Test void sourceSnapshotPreventsCallbackIntroducedWork() throws Exception {
        TestMenu source = new TestMenu(2).put(0, Items.DIRT, 3).put(1, Items.SAND, 4);
        source.slots.set(0, new Slot(source.storage, 0, 0, 0) {
            @Override public void onTake(Player player, ItemStack taken) { source.put(1, Items.GOLD_INGOT, 9); }
        });
        TestMenu target = new TestMenu(4).put(0, Items.DIRT, 1).put(1, Items.GOLD_INGOT, 1);
        sort(source, List.of(target), true, 1);
        assertEquals(9, source.count(Items.GOLD_INGOT)); assertEquals(1, target.count(Items.GOLD_INGOT));
    }
    @Test void outputCallbacksReceiveActualExtractedCount() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.IRON_INGOT, 12);
        int[] quick = {0}; int[] taken = {0};
        source.slots.set(0, new Slot(source.storage, 0, 0, 0) {
            @Override protected void onQuickCraft(ItemStack stack, int count) { quick[0] += count; }
            @Override public void onTake(Player player, ItemStack stack) { taken[0] += stack.getCount(); }
        });
        TestMenu target = new TestMenu(1).put(0, Items.IRON_INGOT, 60);
        sort(source, List.of(target), false, -1);
        assertEquals(4, quick[0]); assertEquals(4, taken[0]); assertEquals(8, source.total());
    }
    @Test void closedDestinationIsSkipped() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.DIRT, 5);
        TestMenu closed = new TestMenu(1).put(0, Items.DIRT, 1); closed.valid = false;
        sort(source, List.of(closed), true, 1); assertEquals(5, source.total()); assertEquals(1, closed.total());
    }
    @Test void duplicatePhysicalSlotCannotReceiveItsOwnItems() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.DIRT, 5);
        TestMenu duplicate = new TestMenu(1); duplicate.slots.set(0, source.slots.get(0));
        sort(source, List.of(duplicate), true, 1); assertEquals(5, source.total());
    }
    @Test void largeStorageDoesNotExhaustTransactionBudget() throws Exception {
        TestMenu source = new TestMenu(36);
        for (int i = 0; i < 36; i++) source.put(i, Items.DIRT, 64);
        TestMenu large = new TestMenu(2048);
        sort(source, List.of(large), true, -1);
        assertEquals(0, source.total()); assertEquals(36 * 64, large.total());
    }
    @Test void storageAndProcessorRolesAreExplicit() throws Exception {
        assertTrue(DesktopSortSlots.isStorage((ChestMenu) unsafe.allocateInstance(ChestMenu.class)));
        assertFalse(DesktopSortSlots.isStorage((FurnaceMenu) unsafe.allocateInstance(FurnaceMenu.class)));
        assertTrue(DesktopSortSlots.isSource((BrewingStandMenu) unsafe.allocateInstance(BrewingStandMenu.class)));
        assertFalse(DesktopSortSlots.isSource((CraftingMenu) unsafe.allocateInstance(CraftingMenu.class)));
    }
    @Test void processorSlotsExcludeInputsAndFuel() throws Exception {
        Field slots = AbstractContainerMenu.class.getDeclaredField("slots"); slots.setAccessible(true);
        FurnaceMenu furnace = (FurnaceMenu) unsafe.allocateInstance(FurnaceMenu.class);
        BrewingStandMenu brewing = (BrewingStandMenu) unsafe.allocateInstance(BrewingStandMenu.class);
        TestMenu fixture = new TestMenu(5);
        slots.set(furnace, fixture.slots); slots.set(brewing, fixture.slots);
        for (int i = 0; i < 5; i++) {
            assertEquals(i == 2, DesktopSortSlots.sourceSlot(furnace, fixture.slots.get(i)));
            assertEquals(i < 3, DesktopSortSlots.sourceSlot(brewing, fixture.slots.get(i)));
        }
    }
    @Test void mainInventoryExcludesHotbarAndEquipment() throws Exception {
        var inventory = (net.minecraft.world.entity.player.Inventory) unsafe.allocateInstance(net.minecraft.world.entity.player.Inventory.class);
        Field field = Player.class.getDeclaredField("inventory"); field.setAccessible(true); field.set(player, inventory);
        var slots = net.minecraft.core.NonNullList.<Slot>create();
        for (int i = 0; i < 41; i++) slots.add(new Slot(inventory, i, 0, 0));
        Field menuSlots = AbstractContainerMenu.class.getDeclaredField("slots"); menuSlots.setAccessible(true);
        menuSlots.set(player.inventoryMenu, slots);
        Method method = DesktopContainerSessions.class.getDeclaredMethod("mainInventorySlots", ServerPlayer.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked") List<Slot> selected = (List<Slot>) method.invoke(null, player);
        assertEquals(27, selected.size());
        assertTrue(selected.stream().allMatch(slot -> slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36));
    }
    @Test void destinationSnapshotDoesNotAcquireNewTagMatches() throws Exception {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("sort_test", "metals"));
        Method bind = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class); bind.setAccessible(true);
        var iron = Items.IRON_INGOT.builtInRegistryHolder(); var gold = Items.GOLD_INGOT.builtInRegistryHolder();
        var oldIron = iron.tags().toList(); var oldGold = gold.tags().toList();
        try {
            bind.invoke(iron, List.of(tag)); bind.invoke(gold, List.of(tag));
            TestMenu source = new TestMenu(2).put(0, Items.IRON_INGOT, 5).put(1, Items.GOLD_INGOT, 5);
            TestMenu empty = new TestMenu(2); TestMenu focus = new TestMenu(2).put(0, Items.DIRT, 1);
            // Focus initially rejects iron, which falls back to empty. Gold must still prefer focus,
            // even though the previously empty chest now contains a metal.
            focus.slots.set(1, new Slot(focus.storage, 1, 0, 0) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.GOLD_INGOT); }
            });
            sort(source, List.of(empty, focus), true, 2);
            assertEquals(5, empty.count(Items.IRON_INGOT)); assertEquals(0, empty.count(Items.GOLD_INGOT));
            assertEquals(5, focus.count(Items.GOLD_INGOT));
        } finally { bind.invoke(iron, oldIron); bind.invoke(gold, oldGold); }
    }
    @Test void differentComponentsDoNotMerge() throws Exception {
        TestMenu source = new TestMenu(1).put(0, Items.DIRT, 5);
        source.slots.get(0).getItem().setHoverName(
            net.minecraft.network.chat.Component.literal("Named dirt"));
        TestMenu target = new TestMenu(2).put(0, Items.DIRT, 2);
        sort(source, List.of(target), false, -1);
        assertEquals(2, target.slots.get(0).getItem().getCount());
        assertEquals(5, target.slots.get(1).getItem().getCount());
        assertEquals("Named dirt", target.slots.get(1).getItem().getHoverName().getString());
    }

    @Test void duplicateNetworkDestinationsAreRejected() {
        var source = new DesktopSessionReference(0, 1L, 0); var target = new DesktopSessionReference(1, 2L, 0);
        assertThrows(IllegalArgumentException.class, () -> new DesktopSortWindowsPayload(source,
            List.of(target, target), -1, true));
        assertThrows(IllegalArgumentException.class, () -> new DesktopSortWindowsPayload(source, List.of(), -1, true));
    }
    @Test void leftDragDistributesEvenlyLikePreview() {
        assertDragPlacement(AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE, 10);
    }

    @Test void rightDragPlacesOneLikePreview() {
        assertDragPlacement(AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY, 1);
    }

    @Test void cloneDragFillsStacksLikePreview() {
        assertDragPlacement(AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE, 64);
    }

    private void assertDragPlacement(int type, int expected) {
        TestMenu menu = new TestMenu(3);
        ItemStack carried = new ItemStack(Items.DIRT, 32);
        int preview = AbstractContainerMenu.getQuickCraftPlaceCount(new HashSet<>(menu.slots), type, carried);
        assertEquals(expected, preview);
        assertEquals(preview, com.salts_inventory_update.internal.desktop.DesktopMenuSlots.quickCraftPlaceCount(
            menu, menu.slots.get(0), 3, type, carried));
        var defaultPolicy = new com.salts_inventory_update.internal.desktop.DesktopMenuSlots.GesturePolicy() {};
        assertEquals(preview, defaultPolicy.quickCraftPlaceCount(menu, menu.slots.get(0), 3, type, carried));
    }

    static class TestMenu extends AbstractContainerMenu {
        final SimpleContainer storage; boolean valid = true;
        TestMenu(int size) {
            super(MenuType.GENERIC_9x3, 1); storage = new SimpleContainer(size);
            for (int i = 0; i < size; i++) addSlot(new Slot(storage, i, 0, 0));
        }
        TestMenu put(int index, Item item, int count) { storage.setItem(index, new ItemStack(item, count)); return this; }
        int count(Item item) { return slots.stream().map(Slot::getItem).filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum(); }
        int total() { return slots.stream().map(Slot::getItem).mapToInt(ItemStack::getCount).sum(); }
        @Override public boolean stillValid(Player player) { return valid; }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
