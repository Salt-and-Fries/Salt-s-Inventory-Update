package com.salts_inventory_update.api.client.desktop;

import java.util.Optional;

/** Read-only capture geometry for libraries that calculate their own pixel scissors.
 * Empty outside an active native-template render; do not retain it between frames. */
public record DesktopNativeRenderTarget(int pixelWidth, int pixelHeight, double guiScale) {
    public static Optional<DesktopNativeRenderTarget> current() {
        return com.salts_inventory_update.client.automatic.AutomaticNativeScope.captureTarget();
    }
}
