package com.salts_inventory_update.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryExpansionSlot extends Slot {
    private final Player owner;

    public InventoryExpansionSlot(PlayerExtraInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.owner = inventory.owner();
    }

    @Override
    public boolean isActive() {
        return InventoryExpansion.isGameplayActive(this.owner);
    }

    @Override
    public boolean hasItem() {
        return this.isActive() && super.hasItem();
    }

    @Override
    public boolean mayPickup(Player player) {
        return player == this.owner && this.isActive() && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isActive() && super.mayPlace(stack);
    }

    @Override
    public boolean allowModification(Player player) {
        return player == this.owner && this.isActive() && super.allowModification(player);
    }

}
