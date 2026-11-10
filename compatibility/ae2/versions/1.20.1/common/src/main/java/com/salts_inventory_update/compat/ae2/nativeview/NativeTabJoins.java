package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.Map;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.renderer.Rect2i;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;

/** Desktop-only native return styling and background joins; icons always paint last. */
public final class NativeTabJoins {
    private NativeTabJoins() { }

    /** Fill only the attached seam, using actual tab bounds and the owning palette. */
    public static void draw(GuiGraphicsExtractor graphics, Map<?, ?> panels,
            Map<?, ?> tabs, int surface) {
        for (var entry : panels.entrySet()) {
            if (!(entry.getValue() instanceof Rect2i panel)
                    || !(tabs.get(entry.getKey()) instanceof TabButton tab) || !tab.visible) continue;
            if (panel.getY() > tab.getY()) {
                graphics.fill(tab.getX() + 4, panel.getY() - 1,
                        tab.getX() + Math.max(4, tab.getWidth() - 4), panel.getY() + 2, surface);
            } else {
                graphics.fill(panel.getX() - 1, tab.getY() + 4,
                        panel.getX() + 2, tab.getY() + Math.max(4, tab.getHeight() - 4), surface);
            }
        }
    }
}
