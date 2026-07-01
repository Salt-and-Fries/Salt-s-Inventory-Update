package com.salts_inventory_update.compat.jei;

import java.util.List;

import net.minecraft.world.item.ItemStack;

public record JeiRecipeTransferSlot(int inputIndex, int targetSlotId, List<ItemStack> alternatives) {
    public JeiRecipeTransferSlot {
        alternatives = List.copyOf(alternatives);
    }
}
