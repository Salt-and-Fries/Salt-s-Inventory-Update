package com.salts_inventory_update.compat.curios;

/** Same-width row grid. Scrollbar space is reserved only when not every row fits. */
public record CuriosPanelLayout(int columns, int totalRows, int visibleRows, int maxScroll, boolean scrolling) {
    public static final int WINDOW_GAP = 4;

    /** Includes the native visibility toggle's two-pixel overhang beyond its 18px slot cell. */
    public int viewportWidth(int width) {
        return Math.max(1, width - (scrolling ? 21 : 10));
    }

    public static int preferredHeight(int width, int slots) {
        int columns = Math.max(1, (width - 14) / 18);
        return 14 + Math.max(1, (slots + columns - 1) / columns) * 18;
    }
    public static CuriosPanelLayout fit(int width, int height, int slots) {
        int visible = Math.max(1, (height - 14) / 18);
        int columns = Math.max(1, (width - 14) / 18);
        boolean scroll = (slots + columns - 1) / columns > visible;
        if (scroll) columns = Math.max(1, (width - 25) / 18);
        int rows = Math.max(1, (slots + columns - 1) / columns);
        return new CuriosPanelLayout(columns, rows, Math.min(visible, rows), Math.max(0, rows - visible), scroll);
    }
}
