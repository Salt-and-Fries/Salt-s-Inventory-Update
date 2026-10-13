package com.salts_inventory_update.compat.ae2.nativeview;

import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.StyleManager;
import appeng.menu.SlotSemantics;
import appeng.menu.me.networktool.NetworkToolMenu;

/** Compact native AE artwork for the real shared tool menu; no optional API dependency. */
public final class ToolboxCompanionScreen extends AEBaseScreen<NetworkToolMenu> {
    public ToolboxCompanionScreen(NetworkToolMenu menu, Component title) {
        super(menu, menu.getPlayerInventory(), title,
                StyleManager.loadIndependentStyleDoc("/screens/desktop/toolbox_companion.json"));
    }

    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    public void drawBG(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partial) {
        super.drawBG(graphics, x, y, mouseX, mouseY, partial);
        for (var slot : menu.getSlots(SlotSemantics.STORAGE)) {
            Blitter.texture("guis/toolbox.png").src(61, 18, 18, 18)
                    .dest(x + slot.x - 1, y + slot.y - 1).blit(graphics.unwrap());
        }
    }
}
