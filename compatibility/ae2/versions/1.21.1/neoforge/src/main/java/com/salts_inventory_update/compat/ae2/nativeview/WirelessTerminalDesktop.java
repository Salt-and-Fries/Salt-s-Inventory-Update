package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import java.util.List;
import java.util.Map;

/** Reuses AE2WTLib's original controls while SIU owns player equipment/inventory. */
final class WirelessTerminalDesktop {
    private WirelessTerminalDesktop() { }

    static void prepare(AEBaseScreen<?> screen) {
        if (!screen.getClass().getName().equals("de.mari_023.ae2wtlib.wct.WCTScreen")) return;
        // Mutate only this independent desktop document and its new widget set.
        // Original full-screen WCT instances keep their player preview and slots.
        for (String part : List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "OFFHAND")) {
            var semantic = appeng.menu.SlotSemantics.getOrThrow("AE2WTLIB_" + part);
            screen.setSlotsHidden(semantic, true);
        }
        var widgets = (WidgetContainer) field(screen, "widgets");
        ((Map<?, ?>) field(widgets, "compositeWidgets")).remove("player");
        var named = (Map<?, ?>) field(widgets, "widgets");
        named.remove("player");
        // Keep native button instances, callbacks, visibility and ordering.
        // The installed release supplies its scrolling upgrades and WUT selector.
    }

    static int[] initialSize(appeng.menu.AEBaseMenu menu, int[] size) {
        String name = menu.getClass().getName();
        // The modern native toolbar and four encoding modes need enough rows
        // on first open; keep later user resize preferences in AEWindow.
        int height = name.equals("de.mari_023.ae2wtlib.wct.WCTMenu") ? 194
                : name.equals("de.mari_023.ae2wtlib.wet.WETMenu") ? 176 : 0;
        return new int[] { size[0], Math.max(size[1], height) };
    }

    static void slotBackgrounds(AEBaseScreen<?> screen,
            net.minecraft.client.gui.GuiGraphics graphics) {
        // The native fixed screens bake these wells into their inventory atlas.
        // Keep that artwork (and resource-pack replacements), but place it at
        // the independent desktop slots before AE2 paints their live contents.
        String name = screen.getClass().getName();
        if (!name.equals("de.mari_023.ae2wtlib.wct.TrashScreen")
                && !name.equals("de.mari_023.ae2wtlib.wct.magnet_card.MagnetScreen")) return;
        var well = screen.getStyle().getImage("wirelessSlotBackground");
        for (var slot : screen.getMenu().slots) {
            if (screen.desktopSlotVisible(slot) && !screen.desktopExternalSlot(slot)) {
                well.copy().dest(screen.getGuiLeft() + slot.x - 1,
                        screen.getGuiTop() + slot.y - 1, 18, 18).blit(graphics);
            }
        }
    }

    static void finishFrame(AEBaseScreen<?> screen,
            net.minecraft.client.gui.GuiGraphics graphics) {
        if (!screen.getClass().getName().equals("de.mari_023.ae2wtlib.wat.WATScreen")) return;
        var body = screen.desktopContentBounds();
        // The catalog's native inventory transition narrows by 19 pixels.
        // Replace that blank footer band, including the old inward corner,
        // using its own plain full-width rail. The last list row ends above it.
        appeng.client.gui.style.Blitter.texture("guis/patternaccessterminal.png")
                .src(0, 4, 195, 1)
                .dest(body.getX(), body.getY() + body.getHeight() - 16, body.getWidth(), 12)
                .blit(graphics);
    }

    private static Object field(Object owner, String name) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var value = type.getDeclaredField(name);
                value.setAccessible(true);
                return value.get(owner);
            } catch (NoSuchFieldException absent) {
                // Continue through AE2's native screen/widget base classes.
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("AE2WTLib desktop field " + name, error);
            }
        }
        throw new IllegalStateException("Missing AE2WTLib desktop field " + name);
    }
}
