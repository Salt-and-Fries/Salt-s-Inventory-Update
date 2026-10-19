package com.salts_inventory_update.api.client.desktop;

import net.minecraft.world.item.ItemStack;

/** An authoritative missing ingredient in a native menu slot; never an inventory stack. */
public record DesktopRecipeGhost(int slotIndex, ItemStack item) {
    public DesktopRecipeGhost {
        if (slotIndex < 0 || item == null || item.isEmpty()) throw new IllegalArgumentException("Invalid recipe ghost");
        item = item.copyWithCount(1);
    }
    @Override public ItemStack item() { return item.copy(); }
}
