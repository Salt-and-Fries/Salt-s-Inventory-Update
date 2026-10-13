package com.salts_inventory_update.compat.ae2.nativeview.style;

import appeng.client.gui.style.*;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;

/** Completes a resized AE contour with native, resource-pack-aware edge pixels. */
public final class DesktopFrameEdges {
    private DesktopFrameEdges() {
    }

    public static void bottom(GuiGraphicsExtractor graphics, Blitter source, int nativeWidth,
            int x, int y, int width) {
        int sourceY = source.getSrcY() + source.getSrcHeight() - 4;
        source.copy().src(source.getSrcX(), sourceY, 4, 4).dest(x, y).blit(graphics.unwrap());
        for (int dx = 4; dx < width - 4;) {
            int part = Math.min(nativeWidth - 8, width - 4 - dx);
            source.copy().src(source.getSrcX() + 4, sourceY, part, 4)
                    .dest(x + dx, y).blit(graphics.unwrap());
            dx += part;
        }
        source.copy().src(source.getSrcX() + nativeWidth - 4, sourceY, 4, 4)
                .dest(x + width - 4, y).blit(graphics.unwrap());
    }

    public static void bottomRun(GuiGraphicsExtractor graphics, Blitter source, int x, int y, int width) {
        int sourceY = source.getSrcY() + source.getSrcHeight() - 4;
        source.copy().src(source.getSrcX(), sourceY, 4, 4).dest(x, y).blit(graphics.unwrap());
        for (int dx = 4; dx < width;) {
            int part = Math.min(source.getSrcWidth() - 8, width - dx);
            source.copy().src(source.getSrcX() + 4, sourceY, part, 4)
                    .dest(x + dx, y).blit(graphics.unwrap());
            dx += part;
        }
    }

    /** Replace the old inventory transition with a plain side-bordered strip, then close its full width. */
    public static void catalogBottom(GuiGraphicsExtractor graphics, Blitter header, Blitter bottom,
            int x, int y, int width) {
        header.copy().src(header.getSrcX(), header.getSrcY() + 4, width, 4)
                .dest(x, y - 4).blit(graphics.unwrap());
        bottom(graphics, bottom, Math.min(176, bottom.getSrcWidth()), x, y, width);
    }
}
