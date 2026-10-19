package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;

/** One or two horizontal guide rows, measured in the same GUI coordinates as their host. */
public final class CompactGuideLayout {
    public static final double INSET = 4;
    public static final double ENTRY_GAP = 10;
    public static final double ROW_GAP = 3;
    public static final double BOTTOM_MARGIN = 5;
    public static final double CONTENT_GAP = 4;
    public static final double MIN_SINGLE_ROW_SCALE_RATIO = .80;

    public record Size(double width, double height) {
        public Size {
            if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0)
                throw new IllegalArgumentException("Guide sizes must be finite and positive");
        }
    }
    public record Placement(int index, int row, double x, double y, double width, double height) { }
    public record Layout(List<Placement> entries, double scale, double top, int rows, int reservedHeight) {
        public Layout { entries = List.copyOf(entries); }
    }

    private CompactGuideLayout() { }

    public static Layout compute(List<Size> entries, double width, double height, double preferredScale) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || !Double.isFinite(preferredScale)
                || width <= 0 || height <= 0 || preferredScale <= 0)
            throw new IllegalArgumentException("Guide viewport and scale must be finite and positive");
        if (entries.isEmpty()) return new Layout(List.of(), preferredScale, height, 0, 0);
        double usableWidth = Math.max(1, width - INSET * 2);
        int split = entries.size();
        double widest = rowWidth(entries, 0, split);
        // A small reduction keeps common recipe guides below the gameplay HUD instead
        // of lifting a second row through it. Narrow viewports still get readable rows.
        double singleRowScale = Math.min(preferredScale, usableWidth / widest);
        if (entries.size() > 1 && singleRowScale < preferredScale * MIN_SINGLE_ROW_SCALE_RATIO) {
            double bestWidth = Double.POSITIVE_INFINITY;
            for (int candidate = 1; candidate < entries.size(); candidate++) {
                double maximum = Math.max(rowWidth(entries, 0, candidate), rowWidth(entries, candidate, entries.size()));
                if (maximum < bestWidth) { bestWidth = maximum; split = candidate; }
            }
            widest = bestWidth;
        }
        int rows = split == entries.size() ? 1 : 2;
        double firstHeight = rowHeight(entries, 0, split);
        double secondHeight = rows == 1 ? 0 : rowHeight(entries, split, entries.size());
        double nativeHeight = firstHeight + secondHeight + (rows == 1 ? 0 : ROW_GAP);
        // Even a large user-selected guide scale must leave most of a small inventory visible.
        double maximumHeight = Math.max(1, height * .20 - BOTTOM_MARGIN - CONTENT_GAP);
        double scale = Math.min(preferredScale, Math.min(usableWidth / widest, maximumHeight / nativeHeight));
        double top = Math.max(0, height - BOTTOM_MARGIN - nativeHeight * scale);
        List<Placement> result = new ArrayList<>();
        placeRow(result, entries, 0, split, 0, width, top, firstHeight, scale);
        if (rows == 2) placeRow(result, entries, split, entries.size(), 1, width,
            top + (firstHeight + ROW_GAP) * scale, secondHeight, scale);
        int reserved = (int) Math.min(height, Math.ceil(height - top + CONTENT_GAP));
        return new Layout(result, scale, top, rows, reserved);
    }

    private static void placeRow(List<Placement> result, List<Size> entries, int from, int to, int row,
            double viewportWidth, double y, double rowHeight, double scale) {
        double x = (viewportWidth - rowWidth(entries, from, to) * scale) / 2;
        for (int i = from; i < to; i++) {
            Size size = entries.get(i);
            result.add(new Placement(i, row, x, y + (rowHeight - size.height()) * scale / 2,
                size.width() * scale, size.height() * scale));
            x += (size.width() + ENTRY_GAP) * scale;
        }
    }
    private static double rowWidth(List<Size> entries, int from, int to) {
        double width = Math.max(0, to - from - 1) * ENTRY_GAP;
        for (int i = from; i < to; i++) width += entries.get(i).width();
        return width;
    }
    private static double rowHeight(List<Size> entries, int from, int to) {
        double height = 0;
        for (int i = from; i < to; i++) height = Math.max(height, entries.get(i).height());
        return height;
    }
}
