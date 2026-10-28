package com.salts_inventory_update.compat.ironchests;

/** Geometry only. Menu slot identities do not change when a window reflows or scrolls. */
final class IronChestGrid {
    static final int CELL = 18;
    static final int SCROLL_SPACE = 18;
    private IronChestGrid() { }

    record Layout(int columns, int visibleRows, int totalRows, int maxScroll, int gridX, int scrollX, int height) {
        int firstSlot(int scroll) { return clamp(scroll) * columns; }
        int endSlot(int scroll, int size) { return Math.min(size, firstSlot(scroll) + columns * visibleRows); }
        int clamp(int scroll) { return Math.max(0, Math.min(maxScroll, scroll)); }
    }

    static Layout layout(int slots, int width, int height) {
        int rows = Math.max(1, height / CELL);
        int columns = Math.max(1, Math.min(slots, width / CELL));
        int total = (slots + columns - 1) / columns;
        if (total > rows) {
            columns = Math.max(1, Math.min(slots, (width - SCROLL_SPACE) / CELL));
            total = (slots + columns - 1) / columns;
        }
        int max = Math.max(0, total - rows);
        int gridWidth = columns * CELL + (max > 0 ? SCROLL_SPACE : 0);
        int x = Math.max(0, (width - gridWidth) / 2);
        return new Layout(columns, rows, total, max, x, x + columns * CELL + 4, rows * CELL);
    }
}
