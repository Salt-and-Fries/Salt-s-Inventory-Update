package com.salts_inventory_update.controlify.qa.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the common native sprite draw reached by both opaque and alpha Core buttons. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class FeatureVanillaBookSpriteProbeMixin {
    @Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"))
    private void observeVanillaBookSprite(RenderPipeline pipeline, Identifier sprite, int x, int y,
                                         int width, int height, int color, CallbackInfo callback) {
        QaFeatureProbe.vanillaBookSprite(sprite);
    }
}
