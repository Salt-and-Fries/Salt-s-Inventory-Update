package com.salts_inventory_update.api.client.desktop;

/** Positive surface bounds in GUI pixels. Resize calculations preserve the opposite edge. */
public record DesktopWindowBounds(int x, int y, int width, int height) {
    public DesktopWindowBounds {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Surface bounds must be positive");
        Math.addExact(x, width);
        Math.addExact(y, height);
    }

    public DesktopWindowBounds resizeGesture(DesktopHitRole role, int deltaX, int deltaY,
                                              int minWidth, int minHeight, int maxWidth, int maxHeight) {
        if (!role.resizing()) throw new IllegalArgumentException("A resize role is required");
        if (minWidth <= 0 || minHeight <= 0 || maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("Invalid resize constraints");
        }
        int nextWidth = role.resizeX() == 0 ? width
            : bounded((long) width + (long) deltaX * role.resizeX(), minWidth, maxWidth);
        int nextHeight = role.resizeY() == 0 ? height
            : bounded((long) height + (long) deltaY * role.resizeY(), minHeight, maxHeight);
        int nextX = role.resizeX() < 0 ? Math.addExact(x, width - nextWidth) : x;
        int nextY = role.resizeY() < 0 ? Math.addExact(y, height - nextHeight) : y;
        return new DesktopWindowBounds(nextX, nextY, nextWidth, nextHeight);
    }

    private static int bounded(long value, int min, int max) { return (int) Math.max(min, Math.min(max, value)); }
}
