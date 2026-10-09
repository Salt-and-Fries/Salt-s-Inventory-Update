package com.salts_inventory_update.compat.recipebrowser;

import java.util.List;

import net.minecraft.world.item.ItemStack;

public record RecipeBrowserTransferSlot(int inputIndex, int targetSlotId, List<ItemStack> alternatives) {
    public RecipeBrowserTransferSlot {
        alternatives = List.copyOf(alternatives);
    }
}
