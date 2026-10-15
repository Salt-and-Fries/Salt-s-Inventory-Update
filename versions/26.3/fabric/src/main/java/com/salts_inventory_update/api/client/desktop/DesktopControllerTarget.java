package com.salts_inventory_update.api.client.desktop;

/** A native interactive rectangle with its logical menu row and column.
 * Groups keep a catalog, tab strip, recipe choices and actual slots distinct.
 * Presenters transform the rectangle; logical cells remain unscaled.
 */
public record DesktopControllerTarget(String group, int row, int column, int x, int y, int width, int height) {
    public DesktopControllerTarget {
        if (group == null || group.isBlank() || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Controller targets require a group and positive bounds");
        }
    }
}
