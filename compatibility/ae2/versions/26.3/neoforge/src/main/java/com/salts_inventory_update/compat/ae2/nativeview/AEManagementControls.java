package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.api.client.desktop.DesktopWindowState;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

import appeng.client.gui.style.BackgroundGenerator;
import appeng.menu.AEBaseMenu;

/** Small AE-palette controls; their permissions and input remain owned by the public manager. */
final class AEManagementControls {
    static boolean keyPressed(DesktopInputContext<AEBaseMenu, AEWindow.State> context, KeyEvent event) {
        return keyPressed(context.state(), event, context.minecraft().gui.screen());
    }

    static boolean keyPressed(AEWindow.State state, KeyEvent event, net.minecraft.client.gui.screens.Screen screen) {
        if (state.companion || state.handle == null || !state.handle.isOpen()
                || !state.handle.state().keyboardFocused()
                || !state.handle.available(com.salts_inventory_update.api.client.desktop.DesktopWindowAction.FOCUS))
            return false;
        var available = state.managerControls.stream().filter(DesktopControlState::enabled)
                .filter(control -> control.control() != DesktopWindowControl.RESIZE_GRIP).toList();
        if (event.key() == InputConstants.KEY_F10 && event.hasAltDown()) {
            if (available.isEmpty())
                return false;
            state.handle.cancelInput();
            state.content.setFocused(null);
            state.keyboardControl = available.getFirst().control();
            return true;
        }
        if (state.keyboardControl == null)
            return false;
        if (event.key() == InputConstants.KEY_ESCAPE) {
            state.keyboardControl = null;
            return true;
        }
        int index = -1;
        for (int current = 0; current < available.size(); current++) {
            if (available.get(current).control() == state.keyboardControl)
                index = current;
        }
        if (event.key() == InputConstants.KEY_TAB) {
            int next = index + (event.hasShiftDown() ? -1 : 1);
            if (next < 0 || next >= available.size()) {
                state.keyboardControl = null;
                return false;
            } else
                state.keyboardControl = available.get(next).control();
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_SPACE) {
            if (index >= 0) {
                // Route activation through the actual manager control, including Link and Sort,
                // rather than reproducing its action semantics or bypassing its permission checks.
                var bounds = available.get(index).bounds();
                if (screen != null) {
                    var kind = available.get(index).control();
                    if (kind == DesktopWindowControl.ELLIPSIS || kind == DesktopWindowControl.LINK
                            || kind == DesktopWindowControl.SORT_WINDOWS)
                        state.keyboardControl = null;
                    var click = new MouseButtonEvent(bounds.x() + bounds.width() / 2.0,
                            bounds.y() + bounds.height() / 2.0,
                            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    state.activatingHeaderControl = true;
                    try {
                        screen.mouseClicked(click, false);
                        screen.mouseReleased(click);
                    } finally {
                        state.activatingHeaderControl = false;
                    }
                }
            }
            return true;
        }
        return true;
    }

    static Component label(DesktopWindowControl control, DesktopWindowState state) {
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

    static void render(DesktopRenderContext<?, ?> context, List<DesktopControlState> controls) {
        if (controls.isEmpty()) {
            return;
        }
        {}
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
