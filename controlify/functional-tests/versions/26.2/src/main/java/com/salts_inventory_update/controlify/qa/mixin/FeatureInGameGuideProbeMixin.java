package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.isxander.controlify.gui.guide.InGameButtonGuide", remap = false)
public abstract class FeatureInGameGuideProbeMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), remap = false)
    private void begin(GuiGraphicsExtractor graphics, float delta, CallbackInfo callback) { QaNativeGuideProbe.gameplay(true); }
    @Inject(method = "extractRenderState", at = @At("RETURN"), remap = false)
    private void end(GuiGraphicsExtractor graphics, float delta, CallbackInfo callback) { QaNativeGuideProbe.gameplay(false); }
}
