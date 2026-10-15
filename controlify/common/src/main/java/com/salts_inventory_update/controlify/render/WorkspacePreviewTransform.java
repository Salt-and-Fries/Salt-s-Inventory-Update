package com.salts_inventory_update.controlify.render;

/** Coordinates and model size for native previews that do not inherit the GUI pose. */
public record WorkspacePreviewTransform(int x0, int y0, int x1, int y1, float scaleFactor) {
    public static WorkspacePreviewTransform map(int x0, int y0, int x1, int y1,
            double xx, double xy, double tx, double yx, double yy, double ty) {
        AffineScissor bounds = AffineScissor.transform(x0, y0, x1 - x0, y1 - y0,
                xx, xy, tx, yx, yy, ty);
        // Workspace windows use uniform scale. Its length also survives a rotated GUI basis.
        float scale = (float) Math.hypot(xx, yx);
        return new WorkspacePreviewTransform(bounds.x(), bounds.y(),
                bounds.x() + bounds.width(), bounds.y() + bounds.height(), scale);
    }

    public float modelScale(float nativeScale) { return nativeScale * scaleFactor; }
}
