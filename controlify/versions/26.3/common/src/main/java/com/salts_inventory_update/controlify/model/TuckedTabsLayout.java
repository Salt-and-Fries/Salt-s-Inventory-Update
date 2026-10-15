package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A compact HUD row: each retained window owns one evenly spaced cell. */
public final class TuckedTabsLayout {
    public static final int HEIGHT = 18;
    public static final int TEXT_PADDING = 6;
    private static final int MINIMUM_WIDTH = 28;
    private static final int MAXIMUM_WIDTH = 160;

    private TuckedTabsLayout() { }

    public record Tab<K>(K window, double x, double y, int width, int height, double scale) {
        public double displayedWidth() { return width * scale; }
    }

    /** Keeps insertion order and uses the full row without stretching short names. */
    public static <K> List<Tab<K>> compute(int viewportWidth, Map<K, Integer> labelWidths) {
        if (viewportWidth <= 0 || labelWidths.isEmpty()) return List.of();
        double margin = Math.min(6, viewportWidth * 0.05);
        double cell = (viewportWidth - margin * 2) / labelWidths.size();
        double gap = Math.min(4, cell * 0.18);
        List<Tab<K>> result = new ArrayList<>(labelWidths.size());
        int index = 0;
        for (var entry : labelWidths.entrySet()) {
            double desired = Math.max(MINIMUM_WIDTH, Math.min(MAXIMUM_WIDTH,
                    Math.max(0, entry.getValue()) + TEXT_PADDING * 2));
            double available = Math.min(desired, cell - gap);
            // For very crowded rows, shrink the whole tab rather than dropping windows.
            double scale = Math.min(1, available / MINIMUM_WIDTH);
            int width = Math.max(MINIMUM_WIDTH, (int) Math.floor(available / scale));
            double displayedWidth = width * scale;
            double center = margin + (index++ + 0.5) * cell;
            result.add(new Tab<>(entry.getKey(), center - displayedWidth / 2, 2, width, HEIGHT, scale));
        }
        return List.copyOf(result);
    }
}
