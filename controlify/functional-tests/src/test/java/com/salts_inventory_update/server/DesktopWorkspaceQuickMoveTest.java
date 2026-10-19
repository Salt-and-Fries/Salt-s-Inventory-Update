package com.salts_inventory_update.server;

import com.salts_inventory_update.inventory.InventoryExpansionSlot;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.inventory.PlayerExtraInventory;
import com.salts_inventory_update.network.DesktopPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the real server destination selector and bounded transfer transaction for displayed pairs. */
final class DesktopWorkspaceQuickMoveTest {
    private static sun.misc.Unsafe unsafe;
    private WorkspaceTestPlayer player;
    private Object sessions;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        unsafe = (sun.misc.Unsafe) access.get(null);
    }

    @BeforeEach void setup() throws Exception {
        player = (WorkspaceTestPlayer) unsafe.allocateInstance(WorkspaceTestPlayer.class);
        player.inventory = new Inventory(player);
        InventoryMenu menu = (InventoryMenu) unsafe.allocateInstance(InventoryMenu.class);
        menu.setCarried(ItemStack.EMPTY);
        NonNullList<Slot> slots = NonNullList.create();
        SimpleContainer crafting = new SimpleContainer(5);
        slots.add(new Slot(crafting, 0, 0, 0)); // Output, never an insertion destination.
        for (int i = 1; i <= 4; i++) slots.add(new Slot(crafting, i, 0, 0));
        for (int i = 39; i >= 36; i--) slots.add(new Slot(player.inventory, i, 0, 0));
        for (int i = 9; i < 36; i++) slots.add(new Slot(player.inventory, i, 0, 0));
        for (int i = 0; i < 9; i++) slots.add(new Slot(player.inventory, i, 0, 0));
        slots.add(new Slot(player.inventory, 40, 0, 0));
        set(menu, "slots", slots);
        set(player, "inventoryMenu", menu);
        sessions = construct("PlayerSessions");
    }

    @Test void equipmentDestinationIncludesOnlyCharacterInputsArmorAndOffhand() throws Exception {
        TestMenu source = new TestMenu();
        List<Slot> targets = targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT);
        List<Slot> expected = new ArrayList<>(player.inventoryMenu.slots.subList(1, 9));
        expected.add(player.inventoryMenu.slots.get(45));
        assertEquals(expected, targets);
        assertFalse(targets.contains(player.inventoryMenu.slots.getFirst()));
        assertTrue(targets.stream().noneMatch(slot -> slot.container == player.inventory
                && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36));
    }

    @Test void storageDestinationIncludesMainInventoryHotbarAndExpandedSlotsButNoCharacterSlots() throws Exception {
        PlayerExtraInventory expanded = new PlayerExtraInventory(player);
        expanded.resize(2);
        Slot extra0 = new InventoryExpansionSlot(expanded, 0, 0, 0);
        Slot extra1 = new InventoryExpansionSlot(expanded, 1, 0, 0);
        player.inventoryMenu.slots.add(extra0);
        player.inventoryMenu.slots.add(extra1);
        List<Slot> targets = targets(new TestMenu(), DesktopPackets.QUICK_TARGET_PLAYER_STORAGE);
        assertEquals(38, targets.size());
        assertTrue(targets.contains(extra0) && targets.contains(extra1));
        assertEquals(player.inventoryMenu.slots.subList(9, 36), targets.subList(0, 27));
        assertEquals(List.of(extra0, extra1), targets.subList(27, 29));
        assertEquals(player.inventoryMenu.slots.subList(36, 45), targets.subList(29, 38));
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 45}) {
            assertFalse(targets.contains(player.inventoryMenu.slots.get(i)));
        }
    }

    @Test void equipmentQuickMoveDoesNotLeakIntoUndisplayedStorageAndConservesStack() throws Exception {
        TestMenu source = new TestMenu();
        source.slots.getFirst().set(new ItemStack(Items.DIRT, 17));
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertTrue(source.slots.getFirst().getItem().isEmpty());
        assertEquals(17, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)));
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)));
        assertTrue(player.inventoryMenu.getCarried().isEmpty());
    }

    @Test void storageQuickMoveDoesNotLeakIntoCharacterAndExcludesSamePhysicalSource() throws Exception {
        TestMenu source = new TestMenu();
        source.slots.getFirst().set(new ItemStack(Items.DIRT, 17));
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE));
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)));
        assertEquals(17, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)));
        TestMenu alias = new TestMenu();
        alias.slots.set(0, new Slot(player.inventory, 9, 0, 0));
        assertTrue(transfer(alias, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE));
        assertEquals(17, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)),
                "A same-inventory transfer may move to another storage slot but must never duplicate");
    }

    @Test void fullExactDestinationLeavesSourceIntactEvenIfUndisplayedInventoryHasSpace() throws Exception {
        TestMenu source = new TestMenu();
        source.slots.getFirst().set(new ItemStack(Items.DIRT, 17));
        for (Slot target : targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)) {
            target.set(new ItemStack(Items.SAND, 64));
        }
        assertFalse(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(17, source.slots.getFirst().getItem().getCount());
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)));
    }

    @Test void exactSurfaceTargetsRoundTripThroughAuthenticatedBatchPacketAndOutOfRangeIsRejected() {
        for (int kind : new int[]{DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE}) {
            var packet = new DesktopPackets.DesktopQuickMoveAllPayload(101, 1, 303,
                    List.of(new DesktopPackets.DesktopSlotReference(11, 202, 5, 0)), kind,
                    new DesktopPackets.DesktopSessionReference(DesktopPackets.PLAYER_MENU_SESSION, 303, 7));
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                DesktopPackets.DesktopQuickMoveAllPayload.CODEC.encode(buffer, packet);
                var decoded = DesktopPackets.DesktopQuickMoveAllPayload.CODEC.decode(buffer);
                assertEquals(packet, decoded);
                assertEquals(303, decoded.target().sessionToken());
                assertEquals(7, decoded.target().stateId());
                assertEquals(101, decoded.connectionNonce());
            } finally { buffer.release(); }
        }
        assertThrows(IllegalArgumentException.class, () -> new DesktopPackets.DesktopQuickMoveAllPayload(101, 1, 303,
                List.of(new DesktopPackets.DesktopSlotReference(11, 202, 5, 0)), 5,
                new DesktopPackets.DesktopSessionReference(DesktopPackets.PLAYER_MENU_SESSION, 303, 7)));
    }

    @Test void exactSurfaceTargetsCannotBypassNativeOnlySourcePolicy() throws Exception {
        MenuType<?> type = (MenuType<?>) unsafe.allocateInstance(MenuType.class);
        DesktopMenuBindings.register(type, menu -> menu.slots.size(), false, null, null,
                new DesktopMenuBindings.GesturePolicy() {
                    @Override public boolean useNativeQuickMove(AbstractContainerMenu menu) { return true; }
                    @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot) { return false; }
                });
        TestMenu menu = new TestMenu(type);
        Slot slot = menu.slots.getFirst();
        assertEquals(true, invoke("allowsQuickMoveRouting", menu, slot, DesktopPackets.QUICK_TARGET_DEFAULT));
        for (int kind : new int[]{DesktopPackets.QUICK_TARGET_SESSION, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT,
                DesktopPackets.QUICK_TARGET_PLAYER_STORAGE}) {
            assertEquals(false, invoke("allowsQuickMoveRouting", menu, slot, kind));
        }
    }

    @Test void recipeOutputCanReplenishAfterExactlyOneConsumedRecipeWithoutRollbackOrDuplication() throws Exception {
        int[] inputs = {2};
        int[] quickCraft = {0};
        int[] taken = {0};
        AbstractContainerMenu source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4),
                quickCraft, taken, () -> --inputs[0] > 0 ? new ItemStack(Items.OAK_PLANKS, 4) : ItemStack.EMPTY);
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(1, inputs[0]);
        assertEquals(4, quickCraft[0]);
        assertEquals(4, taken[0]);
        assertEquals(4, source.slots.getFirst().getItem().getCount(), "The next recipe output must remain available");
        assertEquals(4, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)));
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)));
        assertEquals(0, player.droppedCount);
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(0, inputs[0]);
        assertEquals(8, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)));
        assertEquals(8, quickCraft[0]);
        assertEquals(8, taken[0]);
        assertTrue(source.slots.getFirst().getItem().isEmpty());
    }

    @Test void partialRecipeOutputPreservesTheNativeUninsertedRemainderAndNextRecipe() throws Exception {
        int[] inputs = {2};
        int[] quickCraft = {0};
        int[] taken = {0};
        AbstractContainerMenu source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4),
                quickCraft, taken, () -> --inputs[0] > 0 ? new ItemStack(Items.OAK_PLANKS, 4) : ItemStack.EMPTY);
        var targets = targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT);
        for (Slot target : targets) target.set(new ItemStack(Items.SAND, 64));
        targets.getFirst().set(new ItemStack(Items.OAK_PLANKS, 62));
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(64, targets.getFirst().getItem().getCount());
        assertEquals(1, inputs[0]);
        assertEquals(2, quickCraft[0]);
        assertEquals(2, taken[0]);
        assertEquals(2, player.droppedCount, "Native recipe consumption must not discard the leftover output");
        assertEquals(4, source.slots.getFirst().getItem().getCount());
        assertTrue(player.inventoryMenu.getCarried().isEmpty());
    }

    @Test void loomOutputRoutesItsActualResultSlotAndRetainsTheNextBannerRecipe() throws Exception {
        int[] banners = {2};
        int[] dye = {2};
        int[] quickCraft = {0};
        int[] taken = {0};
        AbstractContainerMenu source = resultMenu(LoomMenu.class, 3, new ItemStack(Items.WHITE_BANNER),
                quickCraft, taken, () -> {
                    dye[0]--;
                    return --banners[0] > 0 ? new ItemStack(Items.WHITE_BANNER) : ItemStack.EMPTY;
                });
        Object slotSource = construct("SlotSource", 11, source, source.slots.get(3), null);
        assertEquals(true, invoke("usesNativeResultQuickMove", DesktopPackets.QUICK_TARGET_PLAYER_STORAGE, slotSource, 3));
        assertEquals(false, invoke("usesNativeResultQuickMove", DesktopPackets.QUICK_TARGET_PLAYER_STORAGE, slotSource, 0));
        assertEquals(false, invoke("usesNativeResultQuickMove", DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT, slotSource, 3));
        List<Slot> destination = targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT);
        for (Slot target : destination) target.set(new ItemStack(Items.SAND, 64));
        assertFalse(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(2, banners[0]);
        assertEquals(2, dye[0]);
        assertEquals(0, taken[0]);
        destination.getFirst().set(ItemStack.EMPTY);
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(1, banners[0]);
        assertEquals(1, dye[0]);
        assertEquals(1, quickCraft[0]);
        assertEquals(1, taken[0]);
        assertEquals(1, source.slots.get(3).getItem().getCount(), "Stacked loom inputs replenish the next result");
        assertEquals(1, destination.getFirst().getItem().getCount());
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE)));
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(0, banners[0]);
        assertEquals(0, dye[0]);
        assertEquals(2, destination.getFirst().getItem().getCount());
        assertEquals(2, taken[0]);
        assertTrue(source.slots.get(3).getItem().isEmpty());
        assertTrue(player.inventoryMenu.getCarried().isEmpty());
        assertEquals(0, player.droppedCount);
    }

    @Test void furnacePartialTransferAwardsExactlyExtractedCountAndKeepsUnmovedOutput() throws Exception {
        int[] quickCraft = {0};
        int[] taken = {0};
        AbstractContainerMenu source = resultMenu(FurnaceMenu.class, 2, new ItemStack(Items.IRON_INGOT, 5),
                quickCraft, taken, null);
        var targets = targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT);
        for (Slot target : targets) target.set(new ItemStack(Items.SAND, 64));
        targets.getFirst().set(new ItemStack(Items.IRON_INGOT, 61));
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(3, quickCraft[0], "Furnace result XP bookkeeping receives the actual extracted count");
        assertEquals(3, taken[0]);
        assertEquals(2, source.slots.get(2).getItem().getCount());
        assertEquals(0, player.droppedCount, "Unmoved furnace output remains in its source slot");
    }

    @Test void craftingUsesExplicitHotbarPriorityWhileDefaultRetainsNativeShortcut() throws Exception {
        AbstractContainerMenu source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4),
                new int[1], new int[1], null);
        Object slotSource = construct("SlotSource", 11, source, source.slots.getFirst(), null);
        assertEquals(true, invoke("usesNativeResultQuickMove", DesktopPackets.QUICK_TARGET_DEFAULT, slotSource, 0));
        assertEquals(false, invoke("usesNativeResultQuickMove", DesktopPackets.QUICK_TARGET_PLAYER_STORAGE, slotSource, 0));
        for (int kind : new int[]{DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT, DesktopPackets.QUICK_TARGET_SESSION,
                DesktopPackets.QUICK_TARGET_HOTBAR}) {
            assertEquals(false, invoke("usesNativeResultQuickMove", kind, slotSource, 0));
        }
    }

    @Test void craftedOutputUsesEmptyHotbarBeforeMergingIntoMainInventory() throws Exception {
        player.inventoryMenu.getSlot(9).set(new ItemStack(Items.OAK_PLANKS, 12));
        var source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4), new int[1], new int[1], () -> ItemStack.EMPTY);
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE));
        assertEquals(4, player.inventoryMenu.getSlot(36).getItem().getCount());
        assertEquals(12, player.inventoryMenu.getSlot(9).getItem().getCount());
    }

    @Test void contextualCraftTargetsUsePhysicalOwnersAndRejectVirtualDestinations() throws Exception {
        MenuType<?> type = (MenuType<?>) unsafe.allocateInstance(MenuType.class);
        SimpleContainer dummy = new SimpleContainer(1), fullStorage = new SimpleContainer(1), inputStorage = new SimpleContainer(1);
        fullStorage.setItem(0, new ItemStack(Items.SAND, 64));
        inputStorage.setItem(0, new ItemStack(Items.OAK_LOG));
        Slot full = new Slot(dummy, 0, 0, 0) { @Override public ItemStack getItem() { return fullStorage.getItem(0); } };
        Slot alias = new Slot(dummy, 0, 0, 0) { @Override public ItemStack getItem() { return inputStorage.getItem(0); } };
        Slot phantom = new Slot(dummy, 0, 0, 0), free = new Slot(dummy, 0, 0, 0);
        Object inputOwner = new ArrayList<>(), fullOwner = new ArrayList<>(), freeOwner = new Object();
        assertEquals(inputOwner, fullOwner, "Equal-valued inventory handlers still have distinct physical ownership");
        DesktopMenuBindings.register(type, new DesktopMenuBindings.SlotAccess() {
            @Override public int size(AbstractContainerMenu menu) { return menu.slots.size(); }
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu, Slot slot) { return slot == phantom; }
        }, false, null, null, new DesktopMenuBindings.GesturePolicy() {
            @Override public Object physicalOwner(AbstractContainerMenu menu, Slot slot) {
                return slot == full ? fullOwner : slot == alias ? inputOwner : freeOwner;
            }
            @Override public int physicalIndex(AbstractContainerMenu menu, Slot slot) { return 0; }
        });
        TestMenu destination = new TestMenu(type);
        var plannedSources = java.util.Map.of(construct("ContextualSourceKey", inputOwner, 0), ItemStack.EMPTY);
        assertEquals(List.of(free), invoke("contextualCraftTargets", player, destination, new ItemStack(Items.OAK_PLANKS, 4),
            List.of(full, phantom, free), plannedSources),
            "A consumed ingredient cannot create capacity in another dummy-container wrapper or a phantom entry");
        assertEquals(List.of(alias), invoke("contextualCraftTargets", player, destination, new ItemStack(Items.OAK_PLANKS, 4),
            List.of(alias), plannedSources), "A second view of the consumed physical ingredient may use its freed space");
        assertEquals(64, fullStorage.getItem(0).getCount());
        assertEquals(1, inputStorage.getItem(0).getCount(), "Planning never mutates the underlying inventories");
    }


    @Test void recipeWheelCapacitySelectionReservesHotbarBeforeMainMerges() throws Exception {
        player.inventoryMenu.getSlot(9).set(new ItemStack(Items.OAK_PLANKS, 12));
        var source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4), new int[1], new int[1], () -> ItemStack.EMPTY);
        var candidates = targets(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE);
        assertEquals(List.of(player.inventoryMenu.getSlot(36)), invoke("contextualCraftTargets", player, player.inventoryMenu,
            new ItemStack(Items.OAK_PLANKS,4), candidates, java.util.Map.of()));
        for (int i=36;i<45;i++) player.inventoryMenu.getSlot(i).set(new ItemStack(Items.SAND,64));
        player.inventoryMenu.getSlot(36).set(new ItemStack(Items.OAK_PLANKS,63));
        assertEquals(List.of(player.inventoryMenu.getSlot(36),player.inventoryMenu.getSlot(9)), invoke("contextualCraftTargets", player, player.inventoryMenu,
            new ItemStack(Items.OAK_PLANKS,4), candidates, java.util.Map.of()));
    }

    @Test void craftedOutputFillsHotbarCapacityThenOverflowsToMainInventory() throws Exception {
        for (int i = 36; i < 45; i++) player.inventoryMenu.getSlot(i).set(new ItemStack(Items.SAND, 64));
        player.inventoryMenu.getSlot(36).set(new ItemStack(Items.OAK_PLANKS, 63));
        player.inventoryMenu.getSlot(9).set(new ItemStack(Items.OAK_PLANKS, 12));
        var source = resultMenu(CraftingMenu.class, 0, new ItemStack(Items.OAK_PLANKS, 4), new int[1], new int[1], () -> ItemStack.EMPTY);
        assertTrue(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_STORAGE));
        assertEquals(64, player.inventoryMenu.getSlot(36).getItem().getCount());
        assertEquals(15, player.inventoryMenu.getSlot(9).getItem().getCount());
        assertEquals(0, player.droppedCount);
    }


    @Test void ordinaryFakeUiSourceCannotUseVanillaResultException() throws Exception {
        TestMenu source = new TestMenu();
        Slot fake = new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override public boolean isFake() { return true; }
        };
        source.slots.set(0, fake);
        fake.set(new ItemStack(Items.DIRT, 17));
        assertFalse(transfer(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT));
        assertEquals(17, fake.getItem().getCount());
        assertEquals(0, count(targets(source, DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT)));
    }

    @SuppressWarnings("unchecked")
    private List<Slot> targets(AbstractContainerMenu source, int kind) throws Exception {
        Object slotSource = construct("SlotSource", 11, source, resultSourceSlot(source), null);
        return (List<Slot>) invoke("quickMoveTargets", player, sessions, slotSource, kind, DesktopPackets.PLAYER_MENU_SESSION);
    }

    private boolean transfer(AbstractContainerMenu source, int kind) throws Exception {
        Object slotSource = construct("SlotSource", 11, source, resultSourceSlot(source), null);
        Object budget = construct("QuickMoveWorkBudget", 100_000);
        return (Boolean) invoke("moveSlotStack", player, sessions, slotSource, player.inventoryMenu, targets(source, kind), budget);
    }

    private static Slot resultSourceSlot(AbstractContainerMenu source) {
        return source.slots.get(source instanceof FurnaceMenu ? 2 : source instanceof LoomMenu ? 3 : 0);
    }

    private static AbstractContainerMenu resultMenu(Class<? extends AbstractContainerMenu> type, int index, ItemStack result,
            int[] crafted, int[] taken, java.util.function.Supplier<ItemStack> nextRecipe) throws Exception {
        AbstractContainerMenu menu = (AbstractContainerMenu) unsafe.allocateInstance(type);
        NonNullList<Slot> slots = NonNullList.create();
        SimpleContainer backing = new SimpleContainer(index + 1);
        for (int i = 0; i < index; i++) slots.add(new Slot(backing, i, 0, 0));
        Slot output = new Slot(backing, index, 0, 0) {
            @Override public boolean isFake() { return type == CraftingMenu.class; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override protected void onQuickCraft(ItemStack stack, int count) { crafted[0] += count; }
            @Override public void onTake(Player player, ItemStack stack) {
                taken[0] += stack.getCount();
                if (nextRecipe != null) set(nextRecipe.get());
            }
        };
        output.index = index;
        slots.add(output);
        output.set(result);
        set(menu, "slots", slots);
        menu.setCarried(ItemStack.EMPTY);
        return menu;
    }

    private static int count(List<Slot> slots) { return slots.stream().mapToInt(slot -> slot.getItem().getCount()).sum(); }
    private static Object construct(String name, Object... args) throws Exception {
        Class<?> type = Class.forName(DesktopContainerSessions.class.getName() + "$" + name);
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == args.length) {
                constructor.setAccessible(true); return constructor.newInstance(args);
            }
        }
        throw new AssertionError(name);
    }
    private static Object invoke(String name, Object... args) throws Exception {
        Method method = List.of(DesktopContainerSessions.class.getDeclaredMethods()).stream()
                .filter(candidate -> candidate.getName().equals(name) && candidate.getParameterCount() == args.length)
                .findFirst().orElseThrow();
        method.setAccessible(true); return method.invoke(null, args);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value); return;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu() { this(null); }
        TestMenu(MenuType<?> type) { super(type, 11); addSlot(new Slot(new SimpleContainer(1), 0, 0, 0)); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    }

    private static final class WorkspaceTestPlayer extends ServerPlayer {
        private static final Abilities ABILITIES = new Abilities();
        Inventory inventory;
        int droppedCount;
        private WorkspaceTestPlayer() { super(null, null, null, null); }
        @Override public boolean isAlive() { return true; }
        @Override public boolean isSpectator() { return false; }
        @Override public Component getName() { return Component.literal("workspace-quick-move-test"); }
        @Override public Abilities getAbilities() { return ABILITIES; }
        @Override public Inventory getInventory() { return inventory; }
        @Override public boolean hasInfiniteMaterials() { return false; }
        @Override public ItemEntity drop(ItemStack stack, boolean random) { droppedCount += stack.getCount(); return null; }
    }
}
