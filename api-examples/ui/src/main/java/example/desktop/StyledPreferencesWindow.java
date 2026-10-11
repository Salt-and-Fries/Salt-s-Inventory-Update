package example.desktop;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;

/** Custom artwork with manager-owned title, buttons, drag and resize routing. */
public final class StyledPreferencesWindow extends ClientOnlyWindow {
    private static final DesktopChromeMetrics METRICS = new DesktopChromeMetrics(22, 6, 6, 14, 14, 3, 4, 4, 9);
    @Override public DesktopWindowSurface surface(DesktopWindowContext<PreferencesModel, State> context) {
        return new DesktopWindowSurface(DesktopSurfaceMode.SALT, METRICS.contentInsets(6), true,
            List.of(), DesktopViewportPolicy.CLAMP);
    }
    @Override public DesktopWindowDecoration<PreferencesModel, State> decoration(DesktopWindowContext<PreferencesModel, State> context) {
        return new DesktopWindowDecoration<>() {
            @Override public DesktopChromeMetrics metrics() { return METRICS; }
            @Override public void render(DesktopRenderContext<PreferencesModel, State> paint, List<DesktopControlState> controls) {
                int x = paint.windowX(), y = paint.windowY(), w = paint.windowWidth(), h = paint.windowHeight();
                paint.fill(x, y, x + w, y + h, paint.keyboardFocused() ? 0xFF458CC0 : 0xFF425365);
                paint.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF142838);
                paint.fill(x + 1, y + 1, x + w - 1, y + METRICS.titleBarHeight(), 0xFF245776);
                paint.text(title(paint), x + METRICS.titleLeftPadding(), y + METRICS.titleTextY(), 0xFFFFFFFF, false);
                for (var control : controls) {
                    var b = control.bounds();
                    int color = !control.enabled() ? 0xFF444444 : control.pressed() ? 0xFF102030
                        : control.selected() ? 0xFF367F6B : control.hovered() ? 0xFF76B0CF : 0xFF315C78;
                    paint.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), color);
                    paint.text(control.control().name().substring(0, 1), b.x() + 3, b.y() + 3, 0xFFFFFFFF, false);
                }
            }
        };
    }
}
