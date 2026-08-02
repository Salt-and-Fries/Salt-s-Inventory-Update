package com.salts_inventory_update.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryExpansionSlot extends Slot {
    private final PlayerExtraInventory extraInventory;

    public InventoryExpansionSlot(PlayerExtraInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.extraInventory = inventory;
    }

    @Override
    public boolean isActive() {
        return InventoryExpansion.isGameplayEnabled(this.extraInventory.owner());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isActive() && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.isActive() && super.mayPickup(player);
    }
}
