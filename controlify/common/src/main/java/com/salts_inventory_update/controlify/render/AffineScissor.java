package com.salts_inventory_update.controlify.render;

/** GUI-space clipping bounds rounded outward after an affine pose transform. */
public record AffineScissor(int x, int y, int width, int height) {
    public static AffineScissor transform(int x, int y, int width, int height,
            double xx, double xy, double tx, double yx, double yy, double ty) {
        double right = (double) x + Math.max(width, 0);
        double bottom = (double) y + Math.max(height, 0);
        double x1 = xx * x + xy * y + tx;
        double y1 = yx * x + yy * y + ty;
        if (width <= 0 || height <= 0) {
            return new AffineScissor((int) Math.floor(x1), (int) Math.floor(y1), 0, 0);
        }
        double x2 = xx * right + xy * y + tx;
        double y2 = yx * right + yy * y + ty;
        double x3 = xx * x + xy * bottom + tx;
        double y3 = yx * x + yy * bottom + ty;
        double x4 = xx * right + xy * bottom + tx;
        double y4 = yx * right + yy * bottom + ty;
        int left = (int) Math.floor(Math.min(Math.min(x1, x2), Math.min(x3, x4)));
        int top = (int) Math.floor(Math.min(Math.min(y1, y2), Math.min(y3, y4)));
        int transformedRight = (int) Math.ceil(Math.max(Math.max(x1, x2), Math.max(x3, x4)));
        int transformedBottom = (int) Math.ceil(Math.max(Math.max(y1, y2), Math.max(y3, y4)));
        return new AffineScissor(left, top, Math.max(transformedRight - left, 0),
                Math.max(transformedBottom - top, 0));
    }
}
