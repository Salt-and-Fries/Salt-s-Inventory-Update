package com.salts_inventory_update.api.client.desktop;

/** Positive surface dimensions in scaled GUI pixels, including the declared insets. */
public record DesktopWindowSize(int width, int height) {
    public DesktopWindowSize {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Desktop window size must be positive");
    }

    public static DesktopWindowSize of(int width, int height) { return new DesktopWindowSize(width, height); }
}
