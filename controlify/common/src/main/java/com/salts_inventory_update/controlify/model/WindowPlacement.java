package com.salts_inventory_update.controlify.model;

/** Position and uniform scale in GUI coordinates; opacity and highlight range from zero to one. */
public record WindowPlacement(double x, double y, double scale, double opacity, double highlight) {
    public WindowPlacement {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(scale) || scale <= 0
                || !Double.isFinite(opacity) || opacity < 0 || opacity > 1
                || !Double.isFinite(highlight) || highlight < 0 || highlight > 1) {
            throw new IllegalArgumentException("Placement must have finite coordinates and valid visual values");
        }
    }

    public WindowPlacement translate(double dx, double dy) {
        return new WindowPlacement(x + dx, y + dy, scale, opacity, highlight);
    }

    public WindowPlacement withOpacity(double value) {
        return new WindowPlacement(x, y, scale, value, highlight);
    }

    public static WindowPlacement interpolate(WindowPlacement from, WindowPlacement to, double progress) {
        double t = Math.max(0, Math.min(1, progress));
        return new WindowPlacement(mix(from.x, to.x, t), mix(from.y, to.y, t), mix(from.scale, to.scale, t),
                mix(from.opacity, to.opacity, t), mix(from.highlight, to.highlight, t));
    }

    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
}
