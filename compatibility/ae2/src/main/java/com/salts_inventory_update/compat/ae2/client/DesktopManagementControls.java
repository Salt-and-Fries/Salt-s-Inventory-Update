package com.salts_inventory_update.compat.ae2.client;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.api.client.desktop.DesktopWindowState;
import net.minecraft.network.chat.Component;

/** SIU manager controls, consuming public state and the Core control sprite sheet. */
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


    @FunctionalInterface
    public interface ButtonRenderer {
        void draw(DesktopControlState control, DesktopControlSprites.Sprite sprite);
    }

    /** The adapter supplies its version's resource identifier and atlas width. */
    public static void renderButtons(DesktopRenderContext<?, ?> context, List<DesktopControlState> controls,
            ButtonRenderer renderer) {
        boolean shiftDown = context instanceof DesktopInputContext<?, ?> input && input.shiftDown();
        for (var control : controls) {
            var b = control.bounds();
            int x = b.x(), y = b.y(), w = b.width(), h = b.height();
            if (control.control() == DesktopWindowControl.RESIZE_GRIP) {
                context.fill(x, y + h - 1, x + w, y + h, 0xFF878FA5);
                context.fill(x + w - 1, y, x + w, y + h, 0xFF878FA5);
                continue;
            }
            renderer.draw(control, DesktopControlSprites.sprite(control, shiftDown));
            if (control.keyboardFocused() && control.enabled()) {
                context.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF373737);
            }
        }
    }
}
