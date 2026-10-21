package example.companion;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.resources.Identifier;

/** Original 176x64 artwork and GUI pixels, with four opt-in controls inside its existing title. */
public final class CompanionMainWindow implements DesktopWindowDefinition<CompanionMainMenu, Void> {
    @Override public void opened(DesktopWindowContext<CompanionMainMenu, Void> context) { CompanionUiHooks.observer.opened(context); }
    @Override public void tick(DesktopWindowContext<CompanionMainMenu, Void> context) { CompanionUiHooks.observer.tick(context); }
    @Override public void closed(DesktopWindowContext<CompanionMainMenu, Void> context) { CompanionUiHooks.observer.closed(context); }
    @Override public void windowStateChanged(DesktopWindowContext<CompanionMainMenu, Void> context, DesktopWindowState state) { CompanionUiHooks.observer.tick(context); }
    public static final Identifier ARTWORK = Identifier.parse("companion_example:textures/gui/machine.png");
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<CompanionMainMenu> context) { return DesktopWindowSize.of(176, 64); }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<CompanionMainMenu, Void> context) { return DesktopWindowSize.of(176, 64); }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<CompanionMainMenu, Void> context) {
        return DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 176, 64),
            DesktopWindowRegion.interactive(4, 4, 168, 12).withRole(DesktopHitRole.MOVE)));
    }
    @Override public List<DesktopSurfaceControl> managementControls(DesktopWindowContext<CompanionMainMenu, Void> context) {
        return List.of(control(DesktopWindowControl.PIN, 132),
            control(DesktopWindowControl.ELLIPSIS, 146), control(DesktopWindowControl.CLOSE, 160));
    }
    private DesktopSurfaceControl control(DesktopWindowControl action, int x) {
        return new DesktopSurfaceControl(action, new DesktopWindowBounds(x, 5, 11, 11));
    }
    @Override public void render(DesktopRenderContext<CompanionMainMenu, Void> context) {
        paintTitle(context);
        example.desktop.OriginalSlotArtwork.draw(context, 0, context.windowX() + 8, context.windowY() + 32);
        context.text("Shared tool inventory", context.windowX() + 31, context.windowY() + 36, 0xFF333333, false);
    }
    private void paintTitle(DesktopRenderContext<CompanionMainMenu, Void> context) {
        int height = 64;
        context.texture(ARTWORK, context.windowX(), context.windowY(), 0, 0, 176, height, 176, height, 176, 64);
        // Reserve the measured controls and a four-pixel gap; font truncation never resizes the original tabs/artwork.
        int reservedStart = managementControls(context).stream().mapToInt(c -> c.bounds().x()).min().orElse(172);
        context.text(context.trimToWidth("Machine " + context.model().machineNumber, reservedStart - 6 - 4),
            context.windowX() + 6, context.windowY() + 7, 0xFF202020, false);
    }
    @Override public void renderManagementControls(DesktopRenderContext<CompanionMainMenu, Void> context, List<DesktopControlState> controls) {
        CompanionUiHooks.observer.controls(context, controls);
        {}
        for (var state : controls) {
            var bounds = state.bounds();
            int color = !state.enabled() ? 0xFF747474 : state.pressed() ? 0xFF508788
                : state.hovered() || state.keyboardFocused() ? 0xFFCBF3DC : state.selected() ? 0xFF8BDAB0 : 0xFFB3C8BA;
            context.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
            String glyph = switch (state.control()) { case CLOSE -> "x"; case PIN -> "P";  default -> "..."; };
            context.text(glyph, bounds.x() + 2, bounds.y() + 1, 0xFF202020, false);
        }
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<CompanionMainMenu, Void> context, double x, double y) {
        return context.menuSlotHit(0, context.windowX() + 8, context.windowY() + 32, x, y);
    }
    @Override public String layoutRevision(DesktopWindowContext<CompanionMainMenu, Void> context) { return "companion-main-1"; }
}
