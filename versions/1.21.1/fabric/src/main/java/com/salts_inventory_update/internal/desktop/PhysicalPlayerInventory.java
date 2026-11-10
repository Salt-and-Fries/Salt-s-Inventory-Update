package com.salts_inventory_update.internal.desktop;

import com.salts_inventory_update.mixin.accessor.PlayerInventoryAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Slot ownership and item interaction must use the actual inventory even
 * when a cosmetic mod supplies an appearance copy during GUI rendering. */
public final class PhysicalPlayerInventory {
    private PhysicalPlayerInventory() { }
    public static Inventory get(Player player) {
        return player instanceof PlayerInventoryAccessor access
            ? access.salts_inventory_update$getPhysicalInventory() : player.getInventory();
    }
}
