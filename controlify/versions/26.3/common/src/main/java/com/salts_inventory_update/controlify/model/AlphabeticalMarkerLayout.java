package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;

/** Readable letter labels with immutable, exact first-recipe anchors on the continuous ring. */
public final class AlphabeticalMarkerLayout {
    public static final int CENTER_X = 380, CENTER_Y = 221, ANCHOR_RADIUS = 144, GAP = 2;
    public static final Rect CONTENT = new Rect(216, 40, 328, 374);
    public static final int INSIDE_RADIUS = 132, OUTSIDE_RADIUS = 156;
    private static final double CARD_CLEARANCE = 124;
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean intersects(Rect other, int gap) {
            return width > 0 && height > 0 && other.width > 0 && other.height > 0
                && x < other.right() + gap && right() + gap > other.x
                && y < other.bottom() + gap && bottom() + gap > other.y;
        }
        public boolean contains(Rect other) {
            return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
        }
    }
    public record Placement(int anchorX, int anchorY, int labelX, int labelY, int width, int height) {
        public Rect backgroundBounds() { return new Rect(labelX - width / 2, labelY - height / 2, width, height); }
    }
    private AlphabeticalMarkerLayout() { }

    public static List<Placement> layout(List<AlphabeticalRecipeIndex.Group> groups, int recipeCount,
            int width, int height, List<Rect> forbidden) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid alphabetical label size");
        var result = new ArrayList<Placement>(groups.size());
        var occupied = new ArrayList<>(forbidden);
        for (int index = 0; index < groups.size(); index++) {
            var group = groups.get(index);
            double angle = ContextualWheelSelection.outerMarkerAngle(group.firstIndex(), recipeCount);
            int anchorX = radialX(angle, ANCHOR_RADIUS), anchorY = radialY(angle, ANCHOR_RADIUS);
            Placement choice = null;
            // Starting inside keeps A immediately below the tabs instead of displacing it sideways.
            int[] radii = index % 2 == 0 ? new int[]{INSIDE_RADIUS, OUTSIDE_RADIUS, 164}
                : new int[]{OUTSIDE_RADIUS, INSIDE_RADIUS, 164};
            for (int radius : radii) {
                var candidate = new Placement(anchorX, anchorY, radialX(angle, radius), radialY(angle, radius), width, height);
                if (available(candidate.backgroundBounds(), occupied)) { choice = candidate; break; }
            }
            // Only crowded or obstructed labels fan along the tangent; their targets never move.
            if (choice == null) {
                int step = Math.max(width, height) + GAP;
                search: for (int offset = step; offset <= step * 3; offset += step) {
                    for (int radius : radii) for (int sign : new int[]{1, -1}) {
                        int x = radialX(angle, radius) + (int)Math.round(Math.cos(angle) * offset * sign);
                        int y = radialY(angle, radius) + (int)Math.round(Math.sin(angle) * offset * sign);
                        var candidate = new Placement(anchorX, anchorY, x, y, width, height);
                        if (available(candidate.backgroundBounds(), occupied)) { choice = candidate; break search; }
                    }
                }
            }
            // A bounded annular grid handles even synthetic groups with nearly identical anchors.
            if (choice == null) {
                long distance = Long.MAX_VALUE;
                for (int y = CONTENT.y() + height / 2; y + height - height / 2 <= CONTENT.bottom(); y += height + GAP)
                    for (int x = CONTENT.x() + width / 2; x + width - width / 2 <= CONTENT.right(); x += width + GAP) {
                        var candidate = new Placement(anchorX, anchorY, x, y, width, height);
                        long dx = x - anchorX, dy = y - anchorY, cost = dx * dx + dy * dy;
                        if (cost < distance && available(candidate.backgroundBounds(), occupied)) { choice = candidate; distance = cost; }
                    }
            }
            if (choice == null) throw new IllegalArgumentException("Alphabetical label bounds cannot fit all present groups");
            result.add(choice); occupied.add(choice.backgroundBounds());
        }
        return List.copyOf(result);
    }
    private static boolean available(Rect candidate, List<Rect> occupied) {
        if (!CONTENT.contains(candidate)) return false;
        // Keep labels off all carousel-card surfaces; the live ring pointer is a supplied obstacle.
        int nearestX = Math.max(candidate.x(), Math.min(CENTER_X, candidate.right()));
        int nearestY = Math.max(candidate.y(), Math.min(CENTER_Y, candidate.bottom()));
        if (Math.hypot(nearestX - CENTER_X, nearestY - CENTER_Y) < CARD_CLEARANCE) return false;
        return occupied.stream().noneMatch(rect -> candidate.intersects(rect, GAP));
    }
    private static int radialX(double angle, int radius) { return CENTER_X + (int)Math.round(Math.sin(angle) * radius); }
    private static int radialY(double angle, int radius) { return CENTER_Y - (int)Math.round(Math.cos(angle) * radius); }
}
