package com.salts_inventory_update.api.client.desktop;

/** Insets in scaled GUI pixels, included in the surface dimensions. */
public record DesktopInsets(int left, int top, int right, int bottom) {
    public static final DesktopInsets ZERO = new DesktopInsets(0, 0, 0, 0);
    public static final DesktopInsets SALT = new DesktopInsets(8, 24, 8, 8);

    public DesktopInsets {
        if (left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("Insets must be non-negative");
        }
        Math.addExact(left, right);
        Math.addExact(top, bottom);
    }

    public int horizontal() { return Math.addExact(left, right); }
    public int vertical() { return Math.addExact(top, bottom); }

    public DesktopWindowSize surfaceSize(int contentWidth, int contentHeight) {
        if (contentWidth <= 0 || contentHeight <= 0) throw new IllegalArgumentException("Content size must be positive");
        return DesktopWindowSize.of(Math.addExact(contentWidth, horizontal()), Math.addExact(contentHeight, vertical()));
    }

    public int contentWidth(int surfaceWidth) { return Math.max(0, surfaceWidth - horizontal()); }
    public int contentHeight(int surfaceHeight) { return Math.max(0, surfaceHeight - vertical()); }
}
