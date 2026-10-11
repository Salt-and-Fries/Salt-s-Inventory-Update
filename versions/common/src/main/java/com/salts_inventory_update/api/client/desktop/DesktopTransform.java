package com.salts_inventory_update.api.client.desktop;

/** Translation and uniform scale in GUI pixels; input uses the exact inverse of drawing. */
public record DesktopTransform(double x, double y, double scale) {
    public DesktopTransform {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Transform must be finite with positive scale");
        }
    }
    public double screenX(double localX) { return x + localX * scale; }
    public double screenY(double localY) { return y + localY * scale; }
    public double localX(double screenX) { return (screenX - x) / scale; }
    public double localY(double screenY) { return (screenY - y) / scale; }
    public int pixelX(double localX) { return Math.toIntExact(Math.round(screenX(localX))); }
    public int pixelY(double localY) { return Math.toIntExact(Math.round(screenY(localY))); }
}
