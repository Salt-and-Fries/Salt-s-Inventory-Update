package com.salts_inventory_update.api.client.desktop;

/** Immutable paint state for a manager-routed control. No caller mutation changes its behavior. */
public record DesktopControlState(DesktopWindowControl control, DesktopWindowBounds bounds,
                                  boolean hovered, boolean pressed, boolean enabled, boolean selected, boolean keyboardFocused) {
    public DesktopControlState(DesktopWindowControl control, DesktopWindowBounds bounds,
                               boolean hovered, boolean pressed, boolean enabled, boolean selected) {
        this(control, bounds, hovered, pressed, enabled, selected, false);
    }
}
