package example.companion;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.resources.Identifier;

/** Integration-owned title/frame. Only drag is offered; Salt additionally enforces the shared session policy. */
public final class ToolInventoryWindow implements DesktopWindowDefinition<ToolInventoryMenu, Void> {
    @Override public void opened(DesktopWindowContext<ToolInventoryMenu, Void> context) { CompanionUiHooks.observer.opened(context); }
    @Override public void tick(DesktopWindowContext<ToolInventoryMenu, Void> context) { CompanionUiHooks.observer.tick(context); }
    @Override public void closed(DesktopWindowContext<ToolInventoryMenu, Void> context) { CompanionUiHooks.observer.closed(context); }
    @Override public void windowStateChanged(DesktopWindowContext<ToolInventoryMenu, Void> context, DesktopWindowState state) { CompanionUiHooks.observer.tick(context); }
    public static final Identifier ARTWORK = Identifier.parse("companion_example:textures/gui/tool.png");
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<ToolInventoryMenu> context) { return DesktopWindowSize.of(176, 52); }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<ToolInventoryMenu, Void> context) { return DesktopWindowSize.of(176, 52); }
    @Override public DesktopWindowOptions options(DesktopWindowContext<ToolInventoryMenu, Void> context) { return DesktopWindowOptions.DRAG_ONLY; }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<ToolInventoryMenu, Void> context) {
        return DesktopWindowSurface.modOwned(List.of(DesktopWindowRegion.visualAndInteractive(0, 0, 176, 52),
            DesktopWindowRegion.interactive(4, 4, 168, 14).withRole(DesktopHitRole.MOVE)));
    }
    @Override public void render(DesktopRenderContext<ToolInventoryMenu, Void> context) {
        context.texture(ARTWORK, context.windowX(), context.windowY(), 0, 0, 176, 52, 176, 52, 176, 52);
        context.text("Tool upgrades", context.windowX() + 6, context.windowY() + 8, 0xFF202020, false);
        for (int i = 0; i < 9; i++) example.desktop.OriginalSlotArtwork.draw(context, i, context.windowX() + 8 + i * 18, context.windowY() + 27);
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<ToolInventoryMenu, Void> context, double x, double y) {
        for (int i = 0; i < 9; i++) {
            var hit = context.menuSlotHit(i, context.windowX() + 8 + i * 18, context.windowY() + 27, x, y);
            if (hit != null) return hit;
        }
        return null;
    }
    @Override public String layoutRevision(DesktopWindowContext<ToolInventoryMenu, Void> context) { return "companion-tool-1"; }
}
