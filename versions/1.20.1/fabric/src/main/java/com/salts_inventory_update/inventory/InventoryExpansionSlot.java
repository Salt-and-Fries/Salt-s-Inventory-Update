package com.salts_inventory_update.inventory;

import net.minecraft.world.inventory.Slot;

public final class InventoryExpansionSlot extends Slot {
    public InventoryExpansionSlot(PlayerExtraInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
}
