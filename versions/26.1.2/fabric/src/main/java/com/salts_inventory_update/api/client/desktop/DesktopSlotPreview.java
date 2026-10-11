package com.salts_inventory_update.api.client.desktop;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.ItemStack;

/** Detached computed slot presentation. Stack access returns a copy. */
public record DesktopSlotPreview(int sessionId, int menuSlotId, ItemStack displayStack,
                                  @Nullable String countText, boolean eligible, boolean selected, boolean hovered) {
    public DesktopSlotPreview { displayStack = displayStack.copy(); }
    @Override public ItemStack displayStack() { return displayStack.copy(); }
}
