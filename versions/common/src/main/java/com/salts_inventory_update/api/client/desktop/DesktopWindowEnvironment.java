package com.salts_inventory_update.api.client.desktop;

/** Native widgets can invalidate cached layout/assets when this value changes. */
public record DesktopWindowEnvironment(int viewportWidth, int viewportHeight, double guiScale, long resourceRevision) {
    public DesktopWindowEnvironment {
        if (viewportWidth < 0 || viewportHeight < 0 || !Double.isFinite(guiScale) || guiScale <= 0 || resourceRevision < 0) {
            throw new IllegalArgumentException("Invalid desktop environment");
        }
    }
}
