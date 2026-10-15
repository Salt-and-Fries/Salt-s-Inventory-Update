package com.salts_inventory_update.controlify.model;

import java.util.Map;

/** Top HUD space occupied by retained tabs and visible parts of the departing workspace. */
public final class TuckedGuideReservation {
    private static final double GAP = 3;
    private TuckedGuideReservation() { }

    public static int inset(double viewportWidth, double viewportHeight, double tabsBottom,
                             WorkspaceLayout<?> frame, Map<?, WindowSize> sizes) {
        if (viewportWidth <= 0 || viewportHeight <= 0) return 0;
        double bottom = Math.max(0, Math.min(viewportHeight, tabsBottom));
        if (frame != null) {
            for (var entry : frame.windows().entrySet()) {
                WindowSize size = sizes.get(entry.getKey());
                WindowPlacement pane = entry.getValue();
                if (size == null || pane.opacity() < .005) continue;
                double right = pane.x() + size.width() * pane.scale();
                double paneBottom = pane.y() + size.height() * pane.scale();
                double border = pane.highlight() > .001 ? 2 : 0;
                if (right + border <= 0 || pane.x() - border >= viewportWidth
                        || paneBottom + border <= 0 || pane.y() - border >= viewportHeight) continue;
                bottom = Math.max(bottom, Math.min(viewportHeight, paneBottom + border));
            }
            WindowPlacement tuck = frame.tuck();
            var sprite = WorkspacePreviewGeometry.tuck(tuck);
            if (tuck.opacity() > .01 && sprite.x() + sprite.size() > 0 && sprite.x() < viewportWidth
                    && sprite.y() + sprite.size() > 0 && sprite.y() < viewportHeight) {
                bottom = Math.max(bottom, Math.min(viewportHeight, sprite.y() + sprite.size()));
            }
        }
        return bottom <= 0 ? 0 : (int) Math.min(viewportHeight, Math.ceil(bottom + GAP));
    }
}
