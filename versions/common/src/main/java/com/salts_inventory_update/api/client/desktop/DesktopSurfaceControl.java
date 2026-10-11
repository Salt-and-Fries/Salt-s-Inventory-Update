package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;

/** Opt-in manager-routed control. Bounds are immutable GUI pixels relative to the complete surface. */
public record DesktopSurfaceControl(DesktopWindowControl control, DesktopWindowBounds bounds) {
    public DesktopSurfaceControl {
        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(bounds, "bounds");
        if (control != DesktopWindowControl.CLOSE && control != DesktopWindowControl.PIN
            && control != DesktopWindowControl.MINIMIZE && control != DesktopWindowControl.ELLIPSIS) {
            throw new IllegalArgumentException("Surface management controls support CLOSE, PIN, MINIMIZE and ELLIPSIS");
        }
        if (bounds.width() <= 0 || bounds.height() <= 0) throw new IllegalArgumentException("Control size must be positive");
    }
}
