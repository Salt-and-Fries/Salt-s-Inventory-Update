package com.salts_inventory_update.internal.desktop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Internal slot/source adaptation for menus whose logical slots are not all in {@code menu.slots}. */
public final class DesktopMenuSlots {
    private static final Map<MenuType<?>, Access> ACCESS = new LinkedHashMap<>();

    private DesktopMenuSlots() {
    }

    public static synchronized void register(MenuType<?> menuType, SlotAccess slotAccess, boolean requiresOpeningData) {
        register(menuType, slotAccess, requiresOpeningData, null, null, null);
    }

    public static synchronized void register(
        MenuType<?> menuType,
        SlotAccess slotAccess,
        boolean requiresOpeningData,
        @Nullable SourceKeyResolver sourceKeyResolver
    ) {
        register(menuType, slotAccess, requiresOpeningData, sourceKeyResolver, null, null);
    }

    public static synchronized void register(
        MenuType<?> menuType,
        SlotAccess slotAccess,
        boolean requiresOpeningData,
        @Nullable SourceKeyResolver sourceKeyResolver,
        @Nullable SnapshotPreparer snapshotPreparer
    ) {
        register(menuType, slotAccess, requiresOpeningData, sourceKeyResolver, snapshotPreparer, null);
    }

    public static synchronized void register(
        MenuType<?> menuType,
        SlotAccess slotAccess,
        boolean requiresOpeningData,
        @Nullable SourceKeyResolver sourceKeyResolver,
        @Nullable SnapshotPreparer snapshotPreparer,
        @Nullable GesturePolicy gesturePolicy
    ) {
        ACCESS.put(
            Objects.requireNonNull(menuType, "menuType"),
            new Access(
                Objects.requireNonNull(slotAccess, "slotAccess"),
                requiresOpeningData,
                sourceKeyResolver,
                snapshotPreparer,
                gesturePolicy
            )
        );
    }

    /** Gives optional-menu adapters a chance to materialize dynamic slots before snapshot validation. */
    public static void prepareSnapshot(AbstractContainerMenu menu, List<ItemStack> items) {
        Access access = access(menu);
        if (access != null && access.snapshotPreparer != null) {
            access.snapshotPreparer.prepare(menu, items);
        }
    }

    public static int size(AbstractContainerMenu menu) {
        Access access = access(menu);
        return access == null ? menu.slots.size() : access.slots.size(menu);
    }

    public static @Nullable Slot slot(AbstractContainerMenu menu, int slotId) {
        if (slotId < 0) {
            return null;
        }
        Access access = access(menu);
        if (access == null) {
            return slotId < menu.slots.size() ? menu.slots.get(slotId) : null;
        }
        return slotId < access.slots.size(menu) ? access.slots.slot(menu, slotId) : null;
    }

    public static int indexOf(AbstractContainerMenu menu, Slot slot) {
        int size = size(menu);
        for (int index = 0; index < size; index++) {
            if (slot(menu, index) == slot) {
                return index;
            }
        }
        return -1;
    }

    public static List<Slot> all(AbstractContainerMenu menu) {
        int size = size(menu);
        List<Slot> slots = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            Slot slot = slot(menu, index);
            if (slot != null) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    public static boolean requiresOpeningData(AbstractContainerMenu menu) {
        Access access = access(menu);
        return access != null && access.requiresOpeningData;
    }

    public static @Nullable String sourceKey(ServerPlayer player, AbstractContainerMenu menu, byte[] openingData) {
        Access access = access(menu);
        return access == null || access.sourceKeyResolver == null
            ? null
            : access.sourceKeyResolver.resolve(player, menu, openingData.clone());
    }

    public static boolean canQuickCraft(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            ? AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
            : access.gesturePolicy.canQuickCraft(menu, slot, carried);
    }

    public static int quickCraftPlaceCount(
        AbstractContainerMenu menu,
        Slot slot,
        int targetCount,
        int quickCraftType,
        ItemStack carried
    ) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            ? vanillaQuickCraftPlaceCount(targetCount, quickCraftType, carried)
            : access.gesturePolicy.quickCraftPlaceCount(menu, slot, targetCount, quickCraftType, carried);
    }

    public static int quickCraftMaxStackSize(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            ? Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried))
            : access.gesturePolicy.quickCraftMaxStackSize(menu, slot, carried);
    }

    private static int vanillaQuickCraftPlaceCount(int targetCount, int quickCraftType, ItemStack carried) {
        if (targetCount <= 0) {
            return 0;
        }
        return switch (quickCraftType) {
            // Vanilla calls even (left-button) distribution charitable and one-per-slot greedy.
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE -> Math.floorDiv(carried.getCount(), targetCount);
            case AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY -> 1;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE -> carried.getMaxStackSize();
            default -> carried.getCount();
        };
    }

    public static boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, Slot slot) {
        Access access = access(menu);
        return menu.canTakeItemForPickAll(carried, slot)
            && (access == null || access.gesturePolicy == null
                || access.gesturePolicy.allowsPickupAll(menu, carried, slot));
    }

    /**
     * Whether a displayed slot may initiate a desktop-wide pickup-all gesture. This is separate
     * from source eligibility because vanilla permits an empty or non-pickup slot as the anchor.
     */
    public static boolean allowsPickupAllAnchor(AbstractContainerMenu menu, ItemStack carried, Slot slot) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            || access.gesturePolicy.allowsPickupAllAnchor(menu, carried, slot);
    }

    public static int repeatedQuickMoveLimit(AbstractContainerMenu menu, Slot slot, ItemStack stack) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            ? Integer.MAX_VALUE
            : Math.max(0, access.gesturePolicy.repeatedQuickMoveLimit(menu, slot, stack));
    }

    public static boolean useNativeQuickMove(AbstractContainerMenu menu) {
        Access access = access(menu);
        return access != null && access.gesturePolicy != null && access.gesturePolicy.useNativeQuickMove(menu);
    }

    /**
     * Whether Salt may bypass this source slot's menu-specific quick-move implementation when a
     * different desktop window is focused. Upgrade and result slots often have bookkeeping that a
     * generic cross-menu insertion cannot reproduce safely.
     */
    public static boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            || access.gesturePolicy.allowsTargetedQuickMoveSource(menu, slot);
    }

    /**
     * Returns the identity-bearing owner of a slot's actual backing storage.  This must not be
     * inferred from {@link Slot#container}: transfer-backed slots commonly share a dummy vanilla
     * container even though every logical slot points at different storage.
     */
    public static Object physicalOwner(AbstractContainerMenu menu, Slot slot) {
        Access access = access(menu);
        Object owner = access == null || access.gesturePolicy == null
            ? slot.container
            : access.gesturePolicy.physicalOwner(menu, slot);
        return owner == null ? slot.container : owner;
    }

    /** Returns the index within {@link #physicalOwner(AbstractContainerMenu, Slot)}. */
    public static int physicalIndex(AbstractContainerMenu menu, Slot slot) {
        Access access = access(menu);
        return access == null || access.gesturePolicy == null
            ? slot.getContainerSlot()
            : access.gesturePolicy.physicalIndex(menu, slot);
    }

    /** Uses identity, rather than {@code equals}, because inventory handlers are mutable owners. */
    public static boolean isSamePhysicalInventory(
        AbstractContainerMenu firstMenu,
        Slot first,
        AbstractContainerMenu secondMenu,
        Slot second
    ) {
        return physicalOwner(firstMenu, first) == physicalOwner(secondMenu, second);
    }

    public static boolean isSamePhysicalSlot(
        AbstractContainerMenu firstMenu,
        Slot first,
        AbstractContainerMenu secondMenu,
        Slot second
    ) {
        return isSamePhysicalInventory(firstMenu, first, secondMenu, second)
            && physicalIndex(firstMenu, first) == physicalIndex(secondMenu, second);
    }

    private static synchronized @Nullable Access access(AbstractContainerMenu menu) {
        MenuType<?> menuType;
        try {
            menuType = menu.getType();
        } catch (UnsupportedOperationException exception) {
            return null;
        }
        return ACCESS.get(menuType);
    }

    @FunctionalInterface
    public interface SlotAccess {
        int size(AbstractContainerMenu menu);

        default Slot slot(AbstractContainerMenu menu, int slotId) {
            return menu.getSlot(slotId);
        }
    }

    @FunctionalInterface
    public interface SourceKeyResolver {
        @Nullable String resolve(ServerPlayer player, AbstractContainerMenu menu, byte[] openingData);
    }

    @FunctionalInterface
    public interface SnapshotPreparer {
        void prepare(AbstractContainerMenu menu, List<ItemStack> items);
    }

    /** Menu-specific rules needed when one mouse gesture spans several detached menus. */
    public interface GesturePolicy {
        default boolean canQuickCraft(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
            return AbstractContainerMenu.canItemQuickReplace(slot, carried, true);
        }

        default int quickCraftPlaceCount(
            AbstractContainerMenu menu,
            Slot slot,
            int targetCount,
            int quickCraftType,
            ItemStack carried
        ) {
            return vanillaQuickCraftPlaceCount(targetCount, quickCraftType, carried);
        }

        default int quickCraftMaxStackSize(AbstractContainerMenu menu, Slot slot, ItemStack carried) {
            return Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
        }

        default boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, Slot slot) {
            return true;
        }

        default boolean allowsPickupAllAnchor(AbstractContainerMenu menu, ItemStack carried, Slot slot) {
            return true;
        }

        default int repeatedQuickMoveLimit(AbstractContainerMenu menu, Slot slot, ItemStack stack) {
            return Integer.MAX_VALUE;
        }

        default boolean useNativeQuickMove(AbstractContainerMenu menu) {
            return false;
        }

        default boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot) {
            return true;
        }

        default Object physicalOwner(AbstractContainerMenu menu, Slot slot) {
            return slot.container;
        }

        default int physicalIndex(AbstractContainerMenu menu, Slot slot) {
            return slot.getContainerSlot();
        }
    }

    private record Access(
        SlotAccess slots,
        boolean requiresOpeningData,
        @Nullable SourceKeyResolver sourceKeyResolver,
        @Nullable SnapshotPreparer snapshotPreparer,
        @Nullable GesturePolicy gesturePolicy
    ) {
    }
}
