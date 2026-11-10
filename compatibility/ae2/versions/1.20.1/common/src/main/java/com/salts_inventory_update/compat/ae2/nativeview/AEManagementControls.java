package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.api.client.desktop.DesktopWindowState;

import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.salts_inventory_update.compat.ae2.client.DesktopControlSprites;
import com.salts_inventory_update.compat.ae2.client.DesktopManagementControls;

import appeng.menu.AEBaseMenu;

/** Shared SIU control artwork; permissions and input remain owned by the public manager. */
final class AEManagementControls {
    private static final ResourceLocation CONTROLS_TEXTURE = new ResourceLocation("salts_inventory_update", "textures/gui/window_controls_native_view.png");
    private static final int TEXTURE_WIDTH = 143;

    static boolean keyPressed(DesktopInputContext<AEBaseMenu, AEWindow.State> context, KeyEvent event) {
        return keyPressed(context.state(), event, context.minecraft().screen);
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
            state.keyboardControl = available.get(0).control();
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
                            InputConstants.MOUSE_BUTTON_LEFT);
                    state.activatingHeaderControl = true;
                    try {
                        LegacyInput.mouseClicked(screen, click, false);
                        LegacyInput.mouseReleased(screen, click);
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
        DesktopManagementControls.renderButtons(context, controls, (control, sprite) -> {
            var b = control.bounds();
            context.texture(CONTROLS_TEXTURE, b.x(), b.y(), sprite.sourceX(), sprite.sourceY(),
                    b.width(), b.height(), DesktopControlSprites.SIZE, DesktopControlSprites.SIZE,
                    TEXTURE_WIDTH, DesktopControlSprites.HEIGHT);
        });
    }
}
