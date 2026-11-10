package com.salts_inventory_update.compat.ae2.client;

import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;

/** Coordinates in Core's shared window_controls sprite sheet, not AE2 artwork. */
public final class DesktopControlSprites {
    public static final int SIZE = 11;
    public static final int HEIGHT = 3 * SIZE;

    private DesktopControlSprites() { }

    public record Sprite(int column, int row) {
        public int sourceX() { return column * SIZE; }
        public int sourceY() { return row * SIZE; }
    }

    public static Sprite sprite(DesktopControlState state, boolean shiftDown) {
        int column = switch (state.control()) {
            case FOCUS -> 0;
            case SORT_INVENTORY -> 1;
            case CLOSE -> 2;
            case ELLIPSIS -> 3;
            case LOCK -> state.selected() ? 5 : 4;
            case PIN -> state.selected() ? 7 : 6;
            case LINK -> 9;
            case SORT_WINDOWS -> shiftDown && state.enabled() ? 11 : 10;
            case RESIZE_GRIP -> throw new IllegalArgumentException("Resize grip is not a button sprite");
        };
        // Pin/lock state has its own column. Only focus, link and the open menu
        // latch the pressed row, just as Core's ordinary window controls do.
        boolean latched = state.selected() && switch (state.control()) {
            case FOCUS, LINK, ELLIPSIS -> true;
            default -> false;
        };
        int row = !state.enabled() ? 0 : state.pressed() || latched ? 2 : state.hovered() ? 1 : 0;
        return new Sprite(column, row);
    }
}
