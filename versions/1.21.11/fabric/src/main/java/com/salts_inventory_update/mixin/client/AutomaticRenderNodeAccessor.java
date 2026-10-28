package com.salts_inventory_update.mixin.client;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="net.minecraft.client.gui.render.state.GuiRenderState$Node")
public interface AutomaticRenderNodeAccessor {
    @Accessor("elementStates") List<net.minecraft.client.gui.render.state.GuiElementRenderState> salts_inventory_update$elementStates();
    @Accessor("glyphStates") List<net.minecraft.client.gui.render.state.GuiElementRenderState> salts_inventory_update$glyphStates();
    @Accessor("itemStates") List<net.minecraft.client.gui.render.state.GuiItemRenderState> salts_inventory_update$itemStates();
    @Accessor("textStates") List<net.minecraft.client.gui.render.state.GuiTextRenderState> salts_inventory_update$textStates();
    @Accessor("picturesInPictureStates") List<net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState> salts_inventory_update$picturesInPictureStates();
}
