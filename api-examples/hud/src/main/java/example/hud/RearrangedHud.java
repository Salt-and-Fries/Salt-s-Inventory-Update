package example.hud;

import com.salts_inventory_update.api.client.desktop.DesktopHudApi;
import com.salts_inventory_update.api.client.desktop.DesktopHudSlot;
import com.salts_inventory_update.api.client.desktop.DesktopTransform;

/** Match this geometry to your mod's own native HUD renderer. No Core implementation imports. */
public final class RearrangedHud {
    private RearrangedHud() { }

    public static DesktopHudApi.RegistrationHandle install() {
        return DesktopHudApi.register("example:vertical_hotbar", 100, (context, layout) ->
            layout.mapSlots(slot -> {
                if (slot.kind() == DesktopHudSlot.Kind.OFFHAND) {
                    return slot.withTransform(new DesktopTransform(context.viewportWidth() - 32, 20, 1));
                }
                double scale = Math.min(1.25, Math.max(0.25, (context.viewportHeight() - 40.0) / 180));
                return slot.withTransform(new DesktopTransform(20, 20 + slot.index() * 20 * scale, scale));
            }));
    }
}
