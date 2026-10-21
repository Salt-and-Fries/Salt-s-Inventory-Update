package com.salts_inventory_update.api.client.desktop;

/** Current GUI viewport. Providers run on the client thread; dimensions and offsets use GUI units. */
public record DesktopHudContext(int viewportWidth, int viewportHeight, double guiScale) {
    public DesktopHudContext {
        if (viewportWidth < 0 || viewportHeight < 0 || !Double.isFinite(guiScale) || guiScale <= 0) {
            throw new IllegalArgumentException("Invalid HUD viewport");
        }
    }
}
