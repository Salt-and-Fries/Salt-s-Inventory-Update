package example.desktop;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgets;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;

/** Original machine frame, custom slot paint and session-scoped native menu button. */
public final class SimpleMachineWindow implements DesktopWindowDefinition<MachineMenu, Void> {
    private static final int[][] POSITIONS = {{10, 28}, {10, 56}, {110, 42}};
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<MachineMenu> context) { return DesktopWindowSize.of(150, 112); }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<MachineMenu, Void> context) { return DesktopWindowSize.of(150, 112); }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<MachineMenu, Void> context) {
        return DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 150, 112),
            DesktopWindowRegion.interactive(4, 4, 97, 16).withRole(DesktopHitRole.MOVE),
            DesktopWindowRegion.interactive(105, 4, 39, 16).withRole(DesktopHitRole.CONTROL),
            DesktopWindowRegion.interactive(40, 82, 64, 16).withRole(DesktopHitRole.CONTROL)));
    }
    @Override public void render(DesktopRenderContext<MachineMenu, Void> context) {
        int x = context.contentX(), y = context.contentY();
        context.fill(x, y, x + 150, y + 112, 0xFF52758B);
        context.fill(x + 1, y + 1, x + 149, y + 111, 0xFF22343F);
        context.text("Machine", x + 6, y + 8, 0xFFFFFFFF, false);
        for (int id = 0; id < POSITIONS.length; id++) OriginalSlotArtwork.draw(context, id, x + POSITIONS[id][0], y + POSITIONS[id][1]);
        context.fill(x + 40, y + 48, x + 84, y + 55, 0xFF343434);
        context.fill(x + 40, y + 48, x + 40 + 44 * Math.min(100, context.model().progress) / 100, y + 55, 0xFF53B96F);
        DesktopWidgets.renderTextButton(context, Component.literal("Cycle"), x + 40, y + 82, 64, 16, false, false);
        DesktopWidgets.renderTextButton(context, Component.literal("Menu"), x + 105, y + 4, 39, 16, false, false);
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<MachineMenu, Void> context, double x, double y) {
        for (int id = 0; id < POSITIONS.length; id++) {
            DesktopSlotHit hit = context.menuSlotHit(id, context.contentX() + POSITIONS[id][0], context.contentY() + POSITIONS[id][1], x, y);
            if (hit != null) return hit;
        }
        return null;
    }
    @Override public boolean mouseClicked(DesktopInputContext<MachineMenu, Void> context, MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && DesktopWidgets.contains(event.x(), event.y(), context.contentX() + 105, context.contentY() + 4, 39, 16)) {
            return context.windowHandle().request(DesktopWindowAction.MANAGEMENT_MENU);
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && DesktopWidgets.contains(event.x(), event.y(), context.contentX() + 40, context.contentY() + 82, 64, 16)) {
            context.sendMenuButton(1);
            return true;
        }
        return false;
    }
}
