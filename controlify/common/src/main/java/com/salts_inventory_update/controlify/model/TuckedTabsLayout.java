package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/** A compact HUD row: each retained window owns one evenly spaced cell. */
public final class TuckedTabsLayout {
    public static final int WIDTH = 28;
    public static final int HEIGHT = 30;
    public static final int CLIPPED_TOP = 6;

    private TuckedTabsLayout() { }

    public record Tab<K>(K window, double x, double y, int width, int height, double scale) {
        public double displayedWidth() { return width * scale; }
    }

    /** The top border stays above the viewport, leaving an icon tab hanging from its edge. */
    public static <K> List<Tab<K>> compute(int viewportWidth, Collection<K> windows) {
        if (viewportWidth <= 0 || windows.isEmpty()) return List.of();
        double margin = Math.min(6, viewportWidth * 0.05);
        double cell = (viewportWidth - margin * 2) / windows.size();
        double gap = Math.min(4, cell * 0.18);
        List<Tab<K>> result = new ArrayList<>(windows.size());
        int index = 0;
        for (K window : windows) {
            double available = Math.min(WIDTH, cell - gap);
            // For very crowded rows, shrink the whole tab rather than dropping windows.
            double scale = available / WIDTH;
            double displayedWidth = WIDTH * scale;
            double center = margin + (index++ + 0.5) * cell;
            result.add(new Tab<>(window, center - displayedWidth / 2, -CLIPPED_TOP * scale, WIDTH, HEIGHT, scale));
        }
        return List.copyOf(result);
    }
}
