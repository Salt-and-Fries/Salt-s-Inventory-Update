package com.salts_inventory_update.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

/** Package-internal escape hatch used only by loader integrations hosting an existing screen. */
public interface InternalDesktopRenderContext {
    GuiGraphicsExtractor rawGraphics();

    /** Immutable snapshot of Salt's active cross-window quick-craft preview. */
    @Nullable QuickCraftPreview quickCraftPreview();

    record QuickCraftPreview(int type, List<Slot> targetSlots) {
        public QuickCraftPreview {
            targetSlots = List.copyOf(targetSlots);
        }
    }
}
