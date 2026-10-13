package com.salts_inventory_update.internal.desktop;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;

import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.world.inventory.*;

/** Explicit storage/output roles, shared by the client and authoritative server. */
public final class DesktopSortSlots {
    private static final Map<MenuType<?>, BiPredicate<AbstractContainerMenu, Slot>> STORAGE = new HashMap<>();
    private DesktopSortSlots() {}
    public static synchronized void registerStorage(MenuType<?> type, BiPredicate<AbstractContainerMenu, Slot> slots) {
        java.util.Objects.requireNonNull(type); java.util.Objects.requireNonNull(slots);
        var previous = STORAGE.put(type, slots);
        com.salts_inventory_update.integration.DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopSortSlots.class) {
                if (STORAGE.get(type) != slots) return;
                if (previous == null) STORAGE.remove(type);
                else STORAGE.put(type, previous);
            }
        });
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
        int index = DesktopMenuBindings.indexOf(menu, slot);
        if (menu instanceof AbstractFurnaceMenu) return index == 2;
        if (menu instanceof BrewingStandMenu) return index >= 0 && index < 3;
        return storageSlot(menu, slot);
    }
}
