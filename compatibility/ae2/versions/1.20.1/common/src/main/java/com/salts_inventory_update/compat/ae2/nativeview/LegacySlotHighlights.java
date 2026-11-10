package com.salts_inventory_update.compat.ae2.nativeview;

import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.20.1 uses the native translucent well, not later highlight sprites. */
public final class LegacySlotHighlights {
    private LegacySlotHighlights() { }

    public static void render(GuiGraphicsExtractor graphics, Object shader,
            ResourceLocation sprite, int x, int y, int width, int height) {
        if (sprite.getPath().equals("container/slot_highlight_front")) {
            AbstractContainerScreen.renderSlotHighlight(graphics.unwrap(), x + 4, y + 4, 0);
        }
    }
}
