package com.salts_inventory_update.api.client.desktop;

/** Metrics for manager-owned chrome; custom renderers use the same rectangles as input. */
public record DesktopChromeMetrics(int titleBarHeight, int titleLeftPadding, int titleTextY,
                                   int controlWidth, int controlHeight, int controlGap,
                                   int controlRightInset, int controlTopInset, int resizeGripSize) {
    public static final DesktopChromeMetrics SALT = new DesktopChromeMetrics(16, 6, 5, 11, 11, 3, 2, 4, 9);
    public DesktopChromeMetrics {
        if (titleBarHeight <= 0 || titleLeftPadding < 0 || titleTextY < 0 || controlWidth <= 0
            || controlHeight <= 0 || controlGap < 0 || controlRightInset < 0 || controlTopInset < 0
            || resizeGripSize < 0 || (long) controlTopInset + controlHeight > titleBarHeight) {
            throw new IllegalArgumentException("Invalid chrome metrics");
        }
    }
    public int controlsWidth(int count) {
        if (count < 0) throw new IllegalArgumentException("Negative control count");
        return Math.addExact(Math.multiplyExact(count, Math.addExact(controlWidth, controlGap)), controlRightInset);
    }
    public DesktopInsets contentInsets(int padding) {
        if (padding < 0) throw new IllegalArgumentException("Negative padding");
        return new DesktopInsets(padding, Math.addExact(titleBarHeight, padding), padding, padding);
    }
}
