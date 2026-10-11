package com.salts_inventory_update.api.client.desktop;

/** What happens when the requested surface cannot fit the GUI viewport. */
public enum DesktopViewportPolicy {
    /** Fit to the viewport; the integration must lay out its content at the resulting size. */
    CLAMP,
    /** Preserve artwork dimensions and allow overflow. Management remains available through the handle. */
    PRESERVE,
    /** Notify the author to select a compact layout; preserve dimensions until it does. */
    ADAPT
}
