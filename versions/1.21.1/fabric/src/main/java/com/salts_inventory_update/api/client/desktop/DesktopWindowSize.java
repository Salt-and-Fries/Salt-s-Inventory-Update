package com.salts_inventory_update.api.client.desktop;

public record DesktopWindowSize(int width, int height) {
    public DesktopWindowSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Desktop window size must be positive");
        }
    }

    public static DesktopWindowSize of(int width, int height) {
        return new DesktopWindowSize(width, height);
    }
}
