package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public interface ScreenWidgetListsAccessor {
    @Accessor("children")
    List<GuiEventListener> salts_inventory_update$children();

    @Accessor("renderables")
    List<Renderable> salts_inventory_update$renderables();

    @Accessor("narratables")
    List<NarratableEntry> salts_inventory_update$narratables();
}
