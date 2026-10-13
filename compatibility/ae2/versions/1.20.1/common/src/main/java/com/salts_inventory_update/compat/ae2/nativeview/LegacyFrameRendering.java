package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingStatusScreen;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import com.salts_inventory_update.compat.ae2.nativeview.style.DesktopFrameEdges;
import net.minecraft.client.gui.GuiGraphics;

/** Finishes cut legacy panels using the installed AE artwork and native scissor stack. */
public final class LegacyFrameRendering {
    private LegacyFrameRendering() { }

    public static void background(AEBaseScreen<?> screen, GuiGraphicsExtractor graphics,
            int x, int y, int mouseX, int mouseY, float delta) {
        background(screen, graphics.unwrap(), x, y, mouseX, mouseY, delta);
    }

    public static void background(AEBaseScreen<?> screen, GuiGraphics graphics,
            int x, int y, int mouseX, int mouseY, float delta) {
        if (!screen.usesAeDesktopLayout() || screen.desktopInventoryCut() == 0) {
            screen.drawBG(graphics, x, y, mouseX, mouseY, delta);
            return;
        }
        var body = screen.desktopContentBounds();
        // Transparent pixels in the native rounded footer cannot erase an already
        // painted vertical side. The dedicated footer pass paints its own four
        // rows, so leave that entire strip unpainted by the original background.
        clipped(screen, graphics, x, y, mouseX, mouseY, delta,
                body.getX(), body.getY(), body.getWidth(), Math.max(0, body.getHeight() - 4));
    }

    private static void clipped(AEBaseScreen<?> screen, GuiGraphics graphics,
            int x, int y, int mouseX, int mouseY, float delta, int sx, int sy, int w, int h) {
        if (w <= 0 || h <= 0) return;
        graphics.enableScissor(sx, sy, sx + w, sy + h);
        try { screen.drawBG(graphics, x, y, mouseX, mouseY, delta); }
        finally { graphics.disableScissor(); }
    }

    public static void bottom(AEBaseScreen<?> screen, GuiGraphicsExtractor graphics) {
        if (!screen.usesAeDesktopLayout() || screen.desktopInventoryCut() == 0) return;
        var style = screen.getStyle();
        var source = style.getTerminalStyle() == null ? style.getBackground() : style.getTerminalStyle().getBottom();
        if (source == null || source.getSrcHeight() < 4) return;
        var body = screen.desktopContentBounds();
        var segments = screen.desktopSurfaceSegments();
        int width = segments.get(segments.size() - 1).getWidth();
        // AE2 15's player-inventory footer is 176 pixels wide even when its
        // terminal atlas region is 195 pixels wide. Stretch its plain center.
        DesktopFrameEdges.bottom(graphics, source, Math.min(176, source.getSrcWidth()),
                body.getX(), body.getY() + body.getHeight() - 4, width);
    }

    public static void cpuJoin(AEBaseScreen<?> screen, GuiGraphicsExtractor graphics) {
        if (!screen.usesAeDesktopLayout() || !(screen instanceof CraftingStatusScreen status)) return;
        var list = status.cpuSelectionBounds();
        int overlap = list.getX() + list.getWidth();
        if (list.getX() >= 0 || overlap <= 0) return;
        var body = screen.desktopContentBounds();
        var source = screen.getStyle().getBackground();
        if (source == null) return;
        // Native AE textures draw immediately; Minecraft's widget fills can be
        // buffered. Finish them before stitching the shared rail over the top.
        graphics.unwrap().flush();
        // The selector overlaps the main panel by six pixels. Its closed right
        // rail must become the shared panel interior, preserving native colors.
        source.copy().src(source.getSrcX() + 3, source.getSrcY() + 5, 1, 1)
                .dest(body.getX(), body.getY() + list.getY() + 4,
                        overlap, list.getHeight() - 4).blit(graphics.unwrap());
    }
}
