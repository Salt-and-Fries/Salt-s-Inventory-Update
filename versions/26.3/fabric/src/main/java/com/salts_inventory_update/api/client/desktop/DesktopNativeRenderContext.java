package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

/** Native rendering access for reusing existing mod widgets and slot renderers. */
public interface DesktopNativeRenderContext {
    GuiGraphicsExtractor rawGraphics();

}
