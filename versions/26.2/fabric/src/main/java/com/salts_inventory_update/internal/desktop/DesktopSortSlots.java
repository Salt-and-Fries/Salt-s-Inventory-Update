package com.salts_inventory_update.internal.desktop;

import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.world.inventory.*;

/** Explicit storage/output roles, shared by the client and authoritative server. */
public final class DesktopSortSlots {
    private static final Map<MenuType<?>, BiPredicate<AbstractContainerMenu, Slot>> STORAGE = new HashMap<>();
    private DesktopSortSlots() {}
    public static void registerStorage(MenuType<?> type, BiPredicate<AbstractContainerMenu, Slot> slots) {
        STORAGE.put(type, slots);
    }
    public static boolean isStorage(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu
            || menu instanceof HopperMenu || menu instanceof DispenserMenu || adapter(menu) != null;
    }
    private static BiPredicate<AbstractContainerMenu, Slot> adapter(AbstractContainerMenu menu) {
        if (menu instanceof InventoryMenu) return null;
        try { return STORAGE.get(menu.getType()); }
        catch (UnsupportedOperationException ignored) { return null; }
    }
    public static boolean isSource(AbstractContainerMenu menu) {
        return isStorage(menu) || menu instanceof AbstractFurnaceMenu || menu instanceof BrewingStandMenu;
    }
    public static boolean storageSlot(AbstractContainerMenu menu, Slot slot) {
        var adapter = adapter(menu);
        return isStorage(menu) && (adapter == null || adapter.test(menu, slot));
    }
    public static boolean sourceSlot(AbstractContainerMenu menu, Slot slot) {
        int index = DesktopMenuSlots.indexOf(menu, slot);
        if (menu instanceof AbstractFurnaceMenu) return index == 2;
        if (menu instanceof BrewingStandMenu) return index >= 0 && index < 3;
        return storageSlot(menu, slot);
    }
}
