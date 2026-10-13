package com.salts_inventory_update.compat.ae2.client;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

/** Fork-owned slot visuals; native inventory stacks cannot be mutated through presentation. */
public record DesktopSlotPresentation(ItemStack stack, @Nullable String countText, boolean preview) {
    public DesktopSlotPresentation {
        stack = stack.copy();
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }
}
