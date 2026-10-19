package com.salts_inventory_update.controlify.model;

/** Pixel-art/icon extents in the same animated frame as native window surfaces. */
public final class WorkspacePreviewGeometry {
    public static final int TUCK_FRAME_SIZE = 32;
    public static final int MAX_ICON_SIZE = 48;
    public record Square(double x, double y, double size) { }
    private WorkspacePreviewGeometry() { }

    public static Square icon(WindowPlacement placement, WindowSize nativeSize) {
        double width = nativeSize.width() * placement.scale();
        double height = nativeSize.height() * placement.scale();
        double side = Math.min(MAX_ICON_SIZE, Math.min(width, height));
        return new Square(placement.x() + (width - side) / 2,
                placement.y() + (height - side) / 2, side);
    }

    public static Square tuck(WindowPlacement placement) {
        double side = TUCK_FRAME_SIZE * placement.scale();
        return new Square(placement.x() - side / 2, placement.y() - side / 2, side);
    }
}
