package com.salts_inventory_update.api.client.desktop;

/** Highest priority wins; equal-priority regions use the last declared region. */
public enum DesktopHitRole {
    NONE(0, 0, 0),
    MOVE(10, 0, 0),
    RESIZE_NORTH(20, 0, -1), RESIZE_SOUTH(20, 0, 1),
    RESIZE_WEST(20, -1, 0), RESIZE_EAST(20, 1, 0),
    RESIZE_NORTH_WEST(20, -1, -1), RESIZE_NORTH_EAST(20, 1, -1),
    RESIZE_SOUTH_WEST(20, -1, 1), RESIZE_SOUTH_EAST(20, 1, 1),
    CONTENT(5, 0, 0), SLOT(60, 0, 0), CONTROL(70, 0, 0), POPUP(80, 0, 0);

    private final int priority;
    private final int resizeX;
    private final int resizeY;

    DesktopHitRole(int priority, int resizeX, int resizeY) {
        this.priority = priority;
        this.resizeX = resizeX;
        this.resizeY = resizeY;
    }

    public int priority() { return priority; }
    public boolean resizing() { return resizeX != 0 || resizeY != 0; }
    public int resizeX() { return resizeX; }
    public int resizeY() { return resizeY; }
}
