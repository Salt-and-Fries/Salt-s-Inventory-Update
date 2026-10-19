package com.salts_inventory_update.internal.desktop;

import com.salts_inventory_update.inventory.InventoryExpansion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/** Shared eligibility for contextual counts and authoritative ingredient extraction. */
public final class ContextualRecipeSources {
    private ContextualRecipeSources() { }
    public static boolean eligible(Player player, Slot slot, boolean allowEmpty) {
        if (slot == null || !slot.isActive() || slot.isFake() || !slot.mayPickup(player) || DesktopItemSourceLocks.isSlotLocked(player, slot)) return false;
        // Armor/offhand use the equipment container, so the inventory-index check below alone
        // cannot distinguish them from an ordinary open storage container or a menu alias.
        if (player.inventoryMenu != null) for (int index : new int[]{0, 5, 6, 7, 8, 45}) {
            if (index >= player.inventoryMenu.slots.size()) continue;
            Slot protectedSlot = player.inventoryMenu.slots.get(index);
            if (slot.container == protectedSlot.container && slot.getContainerSlot() == protectedSlot.getContainerSlot()) return false;
        }
        if (slot.getItem().isEmpty() && !allowEmpty) return false;
        if (slot.container == player.getInventory()) return slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36;
        if (InventoryExpansion.isExtraSlot(slot)) return true;
        return slot.mayPlace(slot.getItem());
    }
}
