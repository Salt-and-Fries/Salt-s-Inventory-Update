package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopThemeAssets;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.controlify.model.TuckedTabsLayout;
import java.util.Map;
import java.util.UUID;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;


/** Item tabs for live windows, continuing above the viewport while the workspace is tucked. */
final class TuckedWindowTabs {
    private TuckedWindowTabs() { }

    static void render(GuiGraphicsExtractor graphics, Map<UUID, DesktopWorkspaceWindow> windows, DesktopWorkspace desktop) {
        graphics.nextStratum();
        for (var tab : layout(graphics.guiWidth(), windows)) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate((float) tab.x(), (float) tab.y());
                graphics.pose().scale((float) tab.scale(), (float) tab.scale());
                frame(graphics, tab.width(), tab.height());
                graphics.item(desktop.previewIcon(tab.window()), 6, 8);
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
        return TuckedTabsLayout.compute(viewportWidth, windows.keySet());
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
        graphics.blit(null, DesktopThemeAssets.WINDOW, x, y, sourceX, sourceY,
                width, height, sourceWidth, sourceHeight, 11, 11, 0xFFFFFFFF);
    }
}
