package com.salts_inventory_update.mixin.client;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(net.minecraft.client.gui.GuiGraphicsExtractor.class)
public interface AutomaticGraphicsCaptureAccessor {
    @Accessor("guiRenderState") net.minecraft.client.renderer.state.gui.GuiRenderState salts_inventory_update$renderState();
}
