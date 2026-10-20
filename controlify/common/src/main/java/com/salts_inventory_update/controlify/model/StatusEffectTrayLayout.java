package com.salts_inventory_update.controlify.model;

/** A passive tray follows Character without contributing to its window size or scale. */
public record StatusEffectTrayLayout(int x, int y, int width, int height, int columns,
        int capacity, int cellWidth) {
    public static final int ROW_HEIGHT = 34;

    public static StatusEffectTrayLayout place(int viewportWidth, int viewportHeight,
            double characterX, double characterY, double characterWidth, double characterHeight,
            int effects, int cellWidth) {
        if (effects <= 0 || viewportWidth < 48 || viewportHeight < 48) return null;
        cellWidth = Math.max(30, Math.min(cellWidth, viewportWidth - 20));
        int availableWidth = Math.min(viewportWidth - 12, Math.max(cellWidth + 8, (int) characterWidth));
        int columns = Math.max(1, Math.min(effects, (availableWidth - 8) / cellWidth));
        int below = (int) Math.ceil(characterY + characterHeight) + 6;
        int above = (int) Math.floor(characterY) - 6;
        int belowRoom = viewportHeight - 6 - below, aboveRoom = above - 6;
        // Prefer underneath. Use the space above only if no complete row fits below.
        boolean underneath = belowRoom >= ROW_HEIGHT + 8 || aboveRoom < ROW_HEIGHT + 8;
        int room = underneath ? belowRoom : aboveRoom;
        int rows = Math.min((effects + columns - 1) / columns,
                Math.max(1, (Math.min(viewportHeight - 12, room) - 8) / ROW_HEIGHT));
        int width = columns * cellWidth + 8, height = rows * ROW_HEIGHT + 8;
        int x = Math.max(6, Math.min(viewportWidth - width - 6, (int) characterX));
        int y = Math.max(6, Math.min(viewportHeight - height - 6, underneath ? below : above - height));
        return new StatusEffectTrayLayout(x, y, width, height, columns, rows * columns, cellWidth);
    }

    public int cellX(int index) { return x + 4 + index % columns * cellWidth; }
    public int cellY(int index) { return y + 4 + index / columns * ROW_HEIGHT; }
}
