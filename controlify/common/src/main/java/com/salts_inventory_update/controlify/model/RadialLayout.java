package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Responsive presentation of existing windows. All visible untucked bounds stay in the viewport. */
public final class RadialLayout {
    private static final WindowSize FALLBACK_SIZE = new WindowSize(176, 166);
    private static final double HIGHLIGHT_SCALE = 1.12;

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
        double margin = Math.min(12, extent / 12.0);
        double cx = viewportWidth / 2.0;
        double cy = viewportHeight / 2.0;
        double radius = extent * 0.34;
        List<K> order = workspace.radialOrder();
        // A common diagonal limit makes differently sized previews fit without overlap, including
        // the focused window and the fully expanded highlighted preview.
        double previewDiagonal = Math.min(extent * 0.22,
                2.0 * radius * Math.sin((Math.PI * 2.0 - 2 * RadialWorkspace.TUCK_HALF_ANGLE)
                        / (2.0 * order.size())) * 0.85);
        double focusDiagonal = Math.max(extent * 0.02, 2.0 * (radius - previewDiagonal / 2.0 - margin));
        Map<K, WindowPlacement> result = new LinkedHashMap<>();
        WindowSize focusSize = sizes.getOrDefault(workspace.focused(), FALLBACK_SIZE);
        double focusScale = Math.min(sizeMultiplier * 0.75, focusDiagonal / diagonal(focusSize));
        result.put(workspace.focused(), centered(cx, cy, focusSize, focusScale, 1, 0));
        for (int i = 0; i < order.size(); i++) {
            K id = order.get(i);
            WindowSize size = sizes.getOrDefault(id, FALLBACK_SIZE);
            boolean highlighted = workspace.highlighted().filter(id::equals).isPresent();
            double baseScale = Math.min(sizeMultiplier * 0.60, previewDiagonal / (diagonal(size) * HIGHLIGHT_SCALE));
            double scale = baseScale * (highlighted ? HIGHLIGHT_SCALE : 1.0);
            double angle = RadialWorkspace.radialAngle(i, order.size());
            result.put(id, centered(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle),
                    size, scale, highlighted ? 1 : 0.86, highlighted ? 1 : 0));
        }
        WindowPlacement tuck = new WindowPlacement(cx, cy + radius, 1,
                workspace.mode() == RadialWorkspace.Mode.RADIAL ? 1 : 0, workspace.tuckHighlighted() ? 1 : 0);

        if (workspace.mode() == RadialWorkspace.Mode.SIDE_BY_SIDE) {
            result.replaceAll((id, placement) -> placement.withOpacity(0));
            double availableWidth = (viewportWidth - 3.0 * margin) / 2.0;
            double availableHeight = viewportHeight - 2.0 * margin;
            K left = workspace.focused();
            K right = workspace.selected();
            placeSide(result, left, sizes.getOrDefault(left, FALLBACK_SIZE),
                    margin + availableWidth / 2.0, cy, availableWidth, availableHeight, sizeMultiplier);
            placeSide(result, right, sizes.getOrDefault(right, FALLBACK_SIZE),
                    2.0 * margin + availableWidth * 1.5, cy, availableWidth, availableHeight, sizeMultiplier);
        } else if (workspace.mode() == RadialWorkspace.Mode.TUCKED_AWAY) {
            double dy = -viewportHeight - extent;
            result.replaceAll((id, placement) -> placement.translate(0, dy));
            tuck = tuck.translate(0, dy).withOpacity(0);
        }
        if (!workspace.active()) {
            result.replaceAll((id, placement) -> placement.withOpacity(0));
            tuck = tuck.withOpacity(0);
        }
        return new WorkspaceLayout<>(result, tuck);
    }

    private static <K> void placeSide(Map<K, WindowPlacement> result, K id, WindowSize size,
                                      double cx, double cy, double width, double height, double multiplier) {
        double scale = Math.min(multiplier * 1.8, Math.min(width / size.width(), height / size.height()));
        result.put(id, centered(cx, cy, size, scale, 1, 0));
    }

    private static WindowPlacement centered(double cx, double cy, WindowSize size, double scale,
                                              double opacity, double highlight) {
        return new WindowPlacement(cx - size.width() * scale / 2.0, cy - size.height() * scale / 2.0,
                scale, opacity, highlight);
    }

    private static double diagonal(WindowSize size) { return Math.hypot(size.width(), size.height()); }
}
