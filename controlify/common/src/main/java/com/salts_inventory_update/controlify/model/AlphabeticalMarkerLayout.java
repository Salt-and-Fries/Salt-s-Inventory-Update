package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Two ordered circular label tracks; the first-recipe targets never leave their exact ring positions. */
public final class AlphabeticalMarkerLayout {
    public static final int CENTER_X = 380, CENTER_Y = 221, ANCHOR_RADIUS = 144, GAP = 2;
    public static final Rect CONTENT = new Rect(216, 40, 328, 374);
    public static final int INSIDE_RADIUS = 132, OUTSIDE_RADIUS = 156;
    private static final double CARD_CLEARANCE = 124, TAU = Math.PI * 2;
    private static final int TRACK_SAMPLES = 720;
    private static final double STEP = TAU / TRACK_SAMPLES;
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
    private record Label(int index, double target, int track) { }
    private AlphabeticalMarkerLayout() { }

    public static List<Placement> layout(List<AlphabeticalRecipeIndex.Group> groups, int recipeCount,
            int width, int height, List<Rect> forbidden) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid alphabetical label size");
        if (groups.isEmpty()) return List.of();
        var labels = new ArrayList<Label>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            double target = ContextualWheelSelection.outerMarkerAngle(groups.get(i).firstIndex(), recipeCount);
            int track = i % 2;
            // Tabs obstruct the outer north arc. Use the inner track locally instead of
            // pushing a near-north letter a long way around the rim to clear a tab.
            if (!available(trackBounds(target, track, width, height), forbidden)
                    && available(trackBounds(target, 1 - track, width, height), forbidden)) track = 1 - track;
            labels.add(new Label(i, target, track));
        }
        labels.sort(Comparator.comparingDouble(Label::target));

        // Put the seam in the largest empty recipe arc, allowing crowded labels to spread
        // to both sides of their targets, including clusters that straddle north.
        double seam = 0, largestGap = -1;
        for (int i = 0; i < labels.size(); i++) {
            double start = labels.get(i).target(), end = labels.get((i + 1) % labels.size()).target();
            if (i == labels.size() - 1) end += TAU;
            if (end - start > largestGap) { largestGap = end - start; seam = normalize((start + end) / 2); }
        }
        final double cut = seam;
        labels.replaceAll(label -> new Label(label.index(), label.target() < cut ? label.target() + TAU : label.target(), label.track()));
        labels.sort(Comparator.comparingDouble(Label::target));

        // Each pair on the same track receives a full diagonal's clearance. Alternating
        // tracks can share half that gap, except when unusually broad localized labels
        // could collide between the tracks.
        double clearance = Math.hypot(width + GAP, height + GAP) + 1;
        if (clearance >= INSIDE_RADIUS * 2) throw new IllegalArgumentException("Alphabetical labels exceed the wheel tracks");
        double sameTrackGap = 2 * Math.asin(clearance / (INSIDE_RADIUS * 2));
        int sameTrackSteps = Math.max(1, (int)Math.ceil(sameTrackGap / STEP));
        int gapSteps = clearance < OUTSIDE_RADIUS - INSIDE_RADIUS ? (sameTrackSteps + 1) / 2 : sameTrackSteps;
        int seamSteps = sameTrackSteps;
        double startAngle = Math.ceil((seam + seamSteps * STEP / 2) / STEP) * STEP;
        int sampleCount = (int)Math.floor((seam + TAU - seamSteps * STEP / 2 - startAngle) / STEP) + 1;
        if (sampleCount <= 0) throw new IllegalArgumentException("Alphabetical label bounds cannot fit all present groups");

        Placement[][] tracks = new Placement[2][sampleCount];
        boolean[][] allowed = new boolean[2][sampleCount];
        for (int track = 0; track < 2; track++) for (int sample = 0; sample < sampleCount; sample++) {
            double angle = startAngle + sample * STEP;
            int radius = track == 0 ? INSIDE_RADIUS : OUTSIDE_RADIUS;
            var placement = new Placement(0, 0, radialX(angle, radius), radialY(angle, radius), width, height);
            tracks[track][sample] = placement;
            allowed[track][sample] = available(placement.backgroundBounds(), forbidden);
        }

        // A stays directly below the category tabs when that position is free. The other
        // letters keep clockwise order while sharing the smallest necessary angular shift.
        int[] samples = pack(labels, groups, startAngle, sampleCount, gapSteps, sameTrackSteps, allowed, true);
        if (samples == null) samples = pack(labels, groups, startAngle, sampleCount, gapSteps, sameTrackSteps, allowed, false);
        if (samples == null) throw new IllegalArgumentException("Alphabetical label bounds cannot fit all present groups");
        Placement[] result = new Placement[groups.size()];
        for (int i = 0; i < labels.size(); i++) {
            var label = labels.get(i);
            var position = tracks[label.track()][samples[i]];
            double angle = ContextualWheelSelection.outerMarkerAngle(groups.get(label.index()).firstIndex(), recipeCount);
            result[label.index()] = new Placement(radialX(angle, ANCHOR_RADIUS), radialY(angle, ANCHOR_RADIUS),
                position.labelX(), position.labelY(), width, height);
        }
        return List.of(result);
    }

    /** Minimum-displacement ordered packing without tangent offsets, radial jumps, or a freeform grid. */
    private static int[] pack(List<Label> labels, List<AlphabeticalRecipeIndex.Group> groups, double start,
            int samples, int gap, int sameTrackGap, boolean[][] allowed, boolean pinNorthA) {
        double[] previous = new double[samples];
        Arrays.fill(previous, Double.POSITIVE_INFINITY);
        int[][] parents = new int[labels.size()][samples];
        for (int i = 0; i < labels.size(); i++) {
            var label = labels.get(i);
            int requiredGap = i > 0 && labels.get(i - 1).track() == label.track() ? sameTrackGap : gap;
            double[] next = new double[samples];
            Arrays.fill(next, Double.POSITIVE_INFINITY);
            int best = -1;
            for (int j = 0; j < samples; j++) {
                int predecessor = j - requiredGap;
                if (i > 0 && predecessor >= 0 && (best < 0 || previous[predecessor] < previous[best])) best = predecessor;
                if (!allowed[label.track()][j]) continue;
                double angle = start + j * STEP;
                if (pinNorthA && groups.get(label.index()).letter().equals("A")
                        && groups.get(label.index()).firstIndex() == 0 && Math.abs(angle - label.target()) > STEP / 4) continue;
                double cost = (angle - label.target()) * (angle - label.target());
                if (i == 0) next[j] = cost;
                else if (best >= 0 && Double.isFinite(previous[best])) next[j] = previous[best] + cost;
                parents[i][j] = best;
            }
            previous = next;
        }
        int best = -1;
        for (int j = 0; j < samples; j++) if (Double.isFinite(previous[j]) && (best < 0 || previous[j] < previous[best])) best = j;
        if (best < 0) return null;
        int[] result = new int[labels.size()];
        for (int i = result.length - 1; i >= 0; i--) { result[i] = best; best = parents[i][best]; }
        return result;
    }
    private static boolean available(Rect candidate, List<Rect> occupied) {
        if (!CONTENT.contains(candidate)) return false;
        int nearestX = Math.max(candidate.x(), Math.min(CENTER_X, candidate.right()));
        int nearestY = Math.max(candidate.y(), Math.min(CENTER_Y, candidate.bottom()));
        if (Math.hypot(nearestX - CENTER_X, nearestY - CENTER_Y) < CARD_CLEARANCE) return false;
        return occupied.stream().noneMatch(rect -> candidate.intersects(rect, GAP));
    }
    private static double normalize(double angle) { return (angle % TAU + TAU) % TAU; }
    private static Rect trackBounds(double angle, int track, int width, int height) {
        int radius = track == 0 ? INSIDE_RADIUS : OUTSIDE_RADIUS;
        return new Rect(radialX(angle, radius) - width / 2, radialY(angle, radius) - height / 2, width, height);
    }
    private static int radialX(double angle, int radius) { return CENTER_X + (int)Math.round(Math.sin(angle) * radius); }
    private static int radialY(double angle, int radius) { return CENTER_Y - (int)Math.round(Math.cos(angle) * radius); }
}
