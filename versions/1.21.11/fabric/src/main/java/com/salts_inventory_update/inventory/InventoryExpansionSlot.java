package com.salts_inventory_update.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryExpansionSlot extends Slot {
    private final PlayerExtraInventory inventory;

    public InventoryExpansionSlot(PlayerExtraInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.inventory = inventory;
    }

    @Override
    public boolean isActive() {
        return InventoryExpansion.isGameplayActive(this.inventory.owner());
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.isActive() && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isActive() && super.mayPlace(stack);
    }

    @Override
    public boolean allowModification(Player player) {
        return this.isActive() && super.allowModification(player);
    }
}
