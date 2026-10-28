package com.salts_inventory_update.client.automatic;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.client.WindowedInventoryClient;

/** Internal fallback chrome. Explicit native templates keep their own surface and decoration. */
final class AutomaticWindowBars {
    static final DesktopInsets INSETS = new DesktopInsets(6, 20, 6, 16);
    static final DesktopWindowSurface SURFACE = new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        INSETS, true, List.of(), DesktopViewportPolicy.CLAMP);

    private AutomaticWindowBars() { }

    static void render(DesktopRenderContext<?, ?> c, List<DesktopControlState> controls) {
        var frame = WindowedInventoryClient.id("textures/gui/window.png");
        c.windowNineSlice(frame, c.windowX(), c.windowY(), c.windowWidth(), INSETS.top());
        c.windowNineSlice(frame, c.windowX(), c.windowY() + c.windowHeight() - INSETS.bottom(),
            c.windowWidth(), INSETS.bottom());
        int right = controls.stream().filter(s -> s.control() != DesktopWindowControl.RESIZE_GRIP)
            .mapToInt(s -> s.bounds().x() - 3).min().orElse(c.windowX() + c.windowWidth() - 6);
        c.text(c.trimToWidth(c.originalTitle().getString(), Math.max(0, right - c.windowX() - 6)),
            c.windowX() + 6, c.windowY() + 5, 0xFF404040, false);
        var atlas = WindowedInventoryClient.id("textures/gui/window_controls.png");
        boolean shift = c instanceof DesktopInputContext<?, ?> input && input.shiftDown();
        for (var state : controls) {
            int column = switch (state.control()) {
                case FOCUS -> 0;
                case SORT_INVENTORY -> 1;
                case CLOSE -> 2;
                case ELLIPSIS -> 3;
                case LOCK -> state.selected() ? 5 : 4;
                case PIN -> state.selected() ? 7 : 6;
                case LINK -> 9;
                case SORT_WINDOWS -> shift ? 11 : 10;
                case RESIZE_GRIP -> -1;
            };
            if (column < 0) continue;
            int row = state.pressed() || state.selected() && state.control() != DesktopWindowControl.LOCK
                ? 2 : state.hovered() || state.keyboardFocused() ? 1 : 0;
            var b = state.bounds();
            c.texture(atlas, b.x(), b.y(), column * 11, row * 11,
                b.width(), b.height(), 11, 11, 132, 33);
        }
    }
}
