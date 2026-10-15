package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopThemeAssets;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.controlify.model.TuckedTabsLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Read-only names for the live windows retained while the workspace is tucked. */
final class TuckedWindowTabs {
    private TuckedWindowTabs() { }

    static void render(GuiGraphicsExtractor graphics, Map<UUID, DesktopWorkspaceWindow> windows) {
        var font = Minecraft.getInstance().font;
        graphics.nextStratum();
        for (var tab : layout(graphics.guiWidth(), windows)) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate((float) tab.x(), (float) tab.y());
                graphics.pose().scale((float) tab.scale(), (float) tab.scale());
                frame(graphics, tab.width(), tab.height());
                String title = windows.get(tab.window()).title();
                int textWidth = tab.width() - TuckedTabsLayout.TEXT_PADDING * 2;
                if (font.width(title) > textWidth) {
                    String ellipsis = "…";
                    title = font.plainSubstrByWidth(title, Math.max(0, textWidth - font.width(ellipsis))) + ellipsis;
                }
                graphics.text(font, title, (tab.width() - font.width(title)) / 2, 5, 0xFF111111, false);
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }

    /** Use the same compressed tab geometry for native top-aligned guide clearance. */
    static double reservedBottom(int viewportWidth, Map<UUID, DesktopWorkspaceWindow> windows) {
        return layout(viewportWidth, windows).stream().mapToDouble(tab -> tab.y() + tab.height() * tab.scale()).max().orElse(0);
    }

    private static java.util.List<TuckedTabsLayout.Tab<UUID>> layout(int viewportWidth, Map<UUID, DesktopWorkspaceWindow> windows) {
        var font = Minecraft.getInstance().font;
        Map<UUID, Integer> labelWidths = new LinkedHashMap<>();
        windows.forEach((id, window) -> labelWidths.put(id, font.width(window.title())));
        return TuckedTabsLayout.compute(viewportWidth, labelWidths);
    }

    /** Published Core window atlas: five-pixel edges surrounding a one-pixel center. */
    private static void frame(GuiGraphicsExtractor graphics, int width, int height) {
        int edge = 5;
        int centerWidth = width - edge * 2;
        int centerHeight = height - edge * 2;
        region(graphics, 0, 0, 0, 0, edge, edge, edge, edge);
        region(graphics, width - edge, 0, 6, 0, edge, edge, edge, edge);
        region(graphics, 0, height - edge, 0, 6, edge, edge, edge, edge);
        region(graphics, width - edge, height - edge, 6, 6, edge, edge, edge, edge);
        region(graphics, edge, 0, 5, 0, centerWidth, edge, 1, edge);
        region(graphics, edge, height - edge, 5, 6, centerWidth, edge, 1, edge);
        region(graphics, 0, edge, 0, 5, edge, centerHeight, edge, 1);
        region(graphics, width - edge, edge, 6, 5, edge, centerHeight, edge, 1);
        region(graphics, edge, edge, 5, 5, centerWidth, centerHeight, 1, 1);
    }

    private static void region(GuiGraphicsExtractor graphics, int x, int y, int sourceX, int sourceY,
                               int width, int height, int sourceWidth, int sourceHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, DesktopThemeAssets.WINDOW, x, y, sourceX, sourceY,
                width, height, sourceWidth, sourceHeight, 11, 11, 0xFFFFFFFF);
    }
}
