package com.salts_inventory_update.compat.ae2.client;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.api.client.desktop.DesktopWindowState;
import net.minecraft.network.chat.Component;

/** The fork's pixel-exact AE-palette manager controls, consuming public API state. */
public final class DesktopManagementControls {
    private DesktopManagementControls() { }

    public static Component label(DesktopWindowControl control, DesktopWindowState state) {
        String label = switch (control) {
            case FOCUS -> "Focus inventory";
            case PIN -> state.pinned() ? "Unpin" : "Pin";
            case LOCK -> state.locked() ? "Unlock" : "Lock";
            case LINK -> "Link windows";

            case CLOSE -> "Close";
            case SORT_WINDOWS -> "Sort windows";
            default -> "More";
        };
        return Component.translatableWithFallback("gui.ae2.DesktopControl." + label.replace(' ', '_'), label);
    }


    public static void renderButtons(DesktopRenderContext<?, ?> context, List<DesktopControlState> controls) {
        for (var control : controls) {
            var b = control.bounds();
            int x = b.x(), y = b.y(), w = b.width(), h = b.height();
            if (control.control() == com.salts_inventory_update.api.client.desktop.DesktopWindowControl.RESIZE_GRIP) {
                context.fill(x, y + h - 1, x + w, y + h, 0xFF878FA5);
                context.fill(x + w - 1, y, x + w, y + h, 0xFF878FA5);
                continue;
            }
            int fill = !control.enabled() ? 0xFFB4B3BE
                    : control.pressed() ? 0xFF878FA5 : control.hovered() ? 0xFFDEDFE3 : 0xFFCECDD5;
            context.fill(x, y, x + w, y + h, 0xFF413F54);
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
            int light = control.pressed() ? 0xFF878FA5 : 0xFFF2F2F2;
            int dark = control.pressed() ? 0xFFF2F2F2 : 0xFF878FA5;
            context.fill(x + 1, y + 1, x + w - 1, y + 2, light);
            context.fill(x + 1, y + 2, x + 2, y + h - 1, light);
            context.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, dark);
            context.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, dark);
            int ink = !control.enabled() ? 0xFF878FA5 : control.selected() ? 0xFF145A78 : 0xFF413F54;
            String[] pixels = switch (control.control()) {
                case CLOSE -> new String[] { "#...#", ".#.#.", "..#..", ".#.#.", "#...#" };
                case PIN -> new String[] { ".###.", "..#..", ".###.", "..#..", "..#.." };
                case FOCUS -> new String[] { "##.##", "#...#", "..#..", "#...#", "##.##" };
                case LOCK -> control.selected()
                        ? new String[] { ".###.", ".#.#.", "#####", "#.#.#", "#####" }
                        : new String[] { "..##.", "..#..", "#####", "#.#.#", "#####" };
                case LINK -> new String[] { ".##..", "#..#.", ".#.#.", ".#..#", "..##." };
                case SORT_WINDOWS -> new String[] { "#.#.#", "#.#.#", ".....", "##.##", "....." };

                default -> new String[] { ".....", ".....", "#.#.#", ".....", "....." };
            };
            int offset = control.pressed() ? 1 : 0;
            for (int row = 0; row < pixels.length; row++) {
                for (int col = 0; col < pixels[row].length(); col++) {
                    if (pixels[row].charAt(col) == '#') {
                        context.fill(x + 3 + col + offset, y + 3 + row + offset,
                                x + 4 + col + offset, y + 4 + row + offset, ink);
                    }
                }
            }
            if (control.keyboardFocused()) {
                context.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF145A78);
            }
        }
    }
}
