package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 26.3 presentation: readable primary panes, subdued neighbors, and an enlarged radial choice. */
public final class WorkspacePresentationLayout {
    private static final WindowSize FALLBACK_SIZE = new WindowSize(176, 166);

    public <K> WorkspaceLayout<K> compute(RadialWorkspace<K> workspace, Map<K, WindowSize> sizes,
                                          double viewportWidth, double viewportHeight, double sizeMultiplier) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(sizes, "sizes");
        if (!Double.isFinite(viewportWidth) || !Double.isFinite(viewportHeight)
                || viewportWidth <= 0 || viewportHeight <= 0 || !Double.isFinite(sizeMultiplier)
                || sizeMultiplier <= 0) {
            throw new IllegalArgumentException("Viewport dimensions and size multiplier must be finite and positive");
        }
        double extent = Math.min(viewportWidth, viewportHeight);
        double margin = Math.min(12, extent * 0.018);
        double gap = Math.min(4, extent * 0.008);
        double cx = viewportWidth / 2.0;
        double cy = viewportHeight / 2.0;
        double radius = extent * 0.39;
        List<K> order = workspace.radialOrder();
        Map<K, WindowPlacement> result = new LinkedHashMap<>();
        WindowSize focusSize = sizes.getOrDefault(workspace.focused(), FALLBACK_SIZE);
        double focusScale = fit(focusSize, viewportWidth * 0.42, viewportHeight * 0.40, sizeMultiplier * 1.55);
        WindowPlacement focus = centered(cx, cy, focusSize, focusScale, 1, 0);
        // Ordinary choices stay small even with only a few windows. The highlighted choice no longer
        // inherits the shrinking angular budget when many open containers share the wheel.
        double previewDiagonal = Math.min(extent * 0.125,
                2.0 * radius * Math.sin((Math.PI * 2.0 - 2 * RadialWorkspace.TUCK_HALF_ANGLE)
                        / (2.0 * order.size())) * 0.63);
        for (int i = 0; i < order.size(); i++) {
            K id = order.get(i);
            WindowSize size = sizes.getOrDefault(id, FALLBACK_SIZE);
            double angle = RadialWorkspace.radialAngle(i, order.size());
            double scale = Math.min(sizeMultiplier * 0.42, previewDiagonal / Math.hypot(size.width(), size.height()));
            result.put(id, centered(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle),
                    size, scale, 1, 0));
        }
        result.put(workspace.focused(), focus);
        workspace.highlighted().filter(order::contains).ifPresent(id -> {
            WindowSize size = sizes.getOrDefault(id, FALLBACK_SIZE);
            double angle = RadialWorkspace.radialAngle(order.indexOf(id), order.size());
            result.put(id, expandedChoice(focus, focusSize, size, angle, radius,
                    viewportWidth, viewportHeight, margin, gap, sizeMultiplier));
        });
        double tuckFit = Math.min(1, 2 * Math.max(.001, viewportHeight - margin - cy - radius)
                / (WorkspacePreviewGeometry.TUCK_FRAME_SIZE * 1.15));
        WindowPlacement tuck = new WindowPlacement(cx, cy + radius, tuckFit * (workspace.tuckHighlighted() ? 1.15 : .68),
                workspace.mode() == RadialWorkspace.Mode.RADIAL ? 1 : 0, workspace.tuckHighlighted() ? 1 : 0);

        if (workspace.mode() == RadialWorkspace.Mode.SIDE_BY_SIDE) {
            result.replaceAll((id, placement) -> placement.withOpacity(0));
            sideBySide(result, workspace, sizes, viewportWidth, viewportHeight, margin, sizeMultiplier);
        } else if (workspace.mode() == RadialWorkspace.Mode.TUCKED_AWAY) {
            double dy = -viewportHeight - extent;
            result.replaceAll((id, placement) -> placement.translate(0, dy));
            tuck = tuck.translate(0, dy).withOpacity(0);
        }
        if (!workspace.active()) {
            result.replaceAll((id, placement) -> placement.withOpacity(0));
            tuck = tuck.withOpacity(0);
        }
        return constrainRadial(workspace, sizes, new WorkspaceLayout<>(result, tuck), viewportWidth, viewportHeight);
    }

    /** Cap the center on sampled animation frames too, not just the final radial target. */
    public <K> WorkspaceLayout<K> constrainRadial(RadialWorkspace<K> workspace, Map<K, WindowSize> sizes,
            WorkspaceLayout<K> frame, double width, double height) {
        if (workspace.mode() != RadialWorkspace.Mode.RADIAL || !workspace.active()) return frame;
        Map<K, WindowPlacement> placements = new LinkedHashMap<>(frame.windows());
        WindowSize focusSize = sizes.getOrDefault(workspace.focused(), FALLBACK_SIZE);
        WindowPlacement focus = placements.get(workspace.focused());
        double extent = Math.min(width, height), gap = Math.min(4, extent * .008);
        double sourceCx = focus.x() + focusSize.width() * focus.scale() / 2;
        double sourceCy = focus.y() + focusSize.height() * focus.scale() / 2;
        double cx = width / 2, cy = height / 2;
        double ringRadius = extent * .39;
        // Reserve the largest icon, independent of its animated scale or which choice is
        // highlighted. A shrinking former preview must never resize the center pane.
        double iconHalfDiagonal = Math.max(WorkspacePreviewGeometry.MAX_ICON_SIZE,
                WorkspacePreviewGeometry.TUCK_FRAME_SIZE * 1.15) / Math.sqrt(2);
        List<K> order = workspace.radialOrder();
        for (int i = 0; i < order.size(); i++) {
            K id = order.get(i);
            var placement = placements.get(id);
            if (placement == null) continue;
            if (workspace.highlighted().filter(id::equals).isPresent()) {
                placements.put(id, placement.withOpacity(1));
                continue;
            }
            // Icons occupy their ring from the first frame. Moving a former ghost
            // through the center would otherwise intersect even a tiny center pane.
            double angle = RadialWorkspace.radialAngle(i, order.size());
            placement = centered(cx + ringRadius * Math.cos(angle), cy + ringRadius * Math.sin(angle),
                sizes.getOrDefault(id, FALLBACK_SIZE), placement.scale(), 1, placement.highlight());
            placements.put(id, placement);
        }
        double scale = Math.min(focus.scale(), 2 * Math.max(.01, ringRadius - iconHalfDiagonal - gap)
                / Math.hypot(focusSize.width(), focusSize.height()));
        double arrival = 1 - Math.min(1, Math.hypot(sourceCx - cx, sourceCy - cy) / Math.max(1, width * .25));
        scale *= .85 + .15 * arrival;
        placements.put(workspace.focused(), centered(cx, cy, focusSize, scale, 1, focus.highlight()));
        return new WorkspaceLayout<>(placements, frame.tuck());
    }

    /** Negative cycle steps appear above the selection; positive steps appear below it. */
    public static <K> List<K> neighbors(RadialWorkspace<K> workspace, int direction) {
        List<K> order = workspace.radialOrder();
        if (direction == 0 || order.size() < 2) return List.of();
        int index = order.indexOf(workspace.selected());
        int limit = Math.min(2, order.size() - 1);
        return java.util.stream.IntStream.rangeClosed(1, limit)
                .mapToObj(depth -> order.get(Math.floorMod(index + depth * Integer.signum(direction), order.size())))
                .toList();
    }

    private static <K> void sideBySide(Map<K, WindowPlacement> result, RadialWorkspace<K> workspace,
                                       Map<K, WindowSize> sizes, double width, double height,
                                       double margin, double multiplier) {
        double columnWidth = (width - 3.0 * margin) / 2.0;
        double availableHeight = height - 2.0 * margin;
        double cy = height / 2.0;
        double leftCx = margin + columnWidth / 2.0;
        double rightCx = 2.0 * margin + columnWidth * 1.5;
        WindowSize focus = sizes.getOrDefault(workspace.focused(), FALLBACK_SIZE);
        WindowSize selected = sizes.getOrDefault(workspace.selected(), FALLBACK_SIZE);
        result.put(workspace.focused(), centered(leftCx, cy, focus,
                fit(focus, columnWidth, availableHeight, multiplier * 2.4), 1, 0));

        boolean hasNeighbors = workspace.radialOrder().size() > 1;
        double selectedBand = availableHeight * (hasNeighbors ? 0.59 : 1.0);
        double selectedScale = fit(selected, columnWidth, selectedBand, multiplier * 2.4);
        result.put(workspace.selected(), centered(rightCx, cy, selected, selectedScale, 1, 0));
        if (!hasNeighbors) return;
        double firstBand = availableHeight * 0.115;
        double secondBand = availableHeight * 0.055;
        double gap = availableHeight * 0.0175;
        Set<K> assigned = new LinkedHashSet<>();
        assigned.add(workspace.selected());
        // First assign the nearest choice on each side. Sparse cycles never duplicate a container
        // or misrepresent a further neighbor as the immediate bumper destination.
        for (int depth = 1; depth <= 2; depth++) {
            for (int direction : new int[]{-1, 1}) {
                List<K> neighbors = neighbors(workspace, direction);
                if (neighbors.size() < depth) continue;
                K id = neighbors.get(depth - 1);
                if (!assigned.add(id)) continue;
                WindowSize size = sizes.getOrDefault(id, FALLBACK_SIZE);
                double band = depth == 1 ? firstBand : secondBand;
                double distance = selectedBand / 2.0 + gap + firstBand / 2.0;
                if (depth == 2) distance += firstBand / 2.0 + gap + secondBand / 2.0;
                double maximum = selectedScale * (depth == 1 ? 0.36 : 0.18);
                double scale = Math.min(fit(size, columnWidth * (depth == 1 ? 0.64 : 0.42), band, maximum),
                        band * 1.5 / Math.hypot(size.width(), size.height()));
                result.put(id, centered(rightCx, cy + direction * distance, size, scale,
                        depth == 1 ? 0.30 : 0.16, 0));
            }
        }
    }

    private static WindowPlacement expandedChoice(WindowPlacement focus, WindowSize focusSize, WindowSize size,
                                                   double angle, double preferredRadius, double width, double height,
                                                   double margin, double gap, double multiplier) {
        double scale = fit(size, width * 0.24, height * 0.26, multiplier * 1.35);
        double ax = Math.abs(Math.cos(angle));
        double ay = Math.abs(Math.sin(angle));
        double focusHalfWidth = focusSize.width() * focus.scale() / 2.0;
        double focusHalfHeight = focusSize.height() * focus.scale() / 2.0;
        // Extremely wide/tall windows may need slightly less expansion to leave the focused pane
        // unobscured. Find the largest fitting choice while retaining the model's exact stick ray.
        if (!choiceFits(scale, size, ax, ay, focusHalfWidth, focusHalfHeight, width, height, margin, gap)) {
            double lower = 0;
            double upper = scale;
            for (int i = 0; i < 48; i++) {
                double middle = (lower + upper) / 2.0;
                if (choiceFits(middle, size, ax, ay, focusHalfWidth, focusHalfHeight, width, height, margin, gap)) lower = middle;
                else upper = middle;
            }
            scale = Math.max(Double.MIN_NORMAL, lower);
        }
        double halfWidth = size.width() * scale / 2.0;
        double halfHeight = size.height() * scale / 2.0;
        double minimumRadius = Math.min(divide(focusHalfWidth + halfWidth + gap, ax),
                divide(focusHalfHeight + halfHeight + gap, ay));
        double maximumRadius = Math.min(divide(width / 2.0 - margin - halfWidth, ax),
                divide(height / 2.0 - margin - halfHeight, ay));
        double radius = Math.max(minimumRadius, Math.min(preferredRadius, maximumRadius));
        return centered(width / 2.0 + radius * Math.cos(angle), height / 2.0 + radius * Math.sin(angle),
                size, scale, 1, 1);
    }

    private static boolean choiceFits(double scale, WindowSize size, double ax, double ay,
                                      double focusHalfWidth, double focusHalfHeight,
                                      double width, double height, double margin, double gap) {
        double halfWidth = size.width() * scale / 2.0;
        double halfHeight = size.height() * scale / 2.0;
        double minimumRadius = Math.min(divide(focusHalfWidth + halfWidth + gap, ax),
                divide(focusHalfHeight + halfHeight + gap, ay));
        double maximumRadius = Math.min(divide(width / 2.0 - margin - halfWidth, ax),
                divide(height / 2.0 - margin - halfHeight, ay));
        return minimumRadius <= maximumRadius;
    }

    private static double divide(double value, double divisor) {
        return divisor < 1.0e-10 ? Double.POSITIVE_INFINITY : value / divisor;
    }

    private static double fit(WindowSize size, double width, double height, double maximumScale) {
        return Math.min(maximumScale, Math.min(width / size.width(), height / size.height()));
    }

    private static WindowPlacement centered(double cx, double cy, WindowSize size, double scale,
                                              double opacity, double highlight) {
        return new WindowPlacement(cx - size.width() * scale / 2.0, cy - size.height() * scale / 2.0,
                scale, opacity, highlight);
    }
}
