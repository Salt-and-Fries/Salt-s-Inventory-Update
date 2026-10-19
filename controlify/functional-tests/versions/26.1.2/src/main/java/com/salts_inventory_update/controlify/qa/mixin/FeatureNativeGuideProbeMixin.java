package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import dev.isxander.controlify.gui.guide.GuideInstanceImpl;
import dev.isxander.controlify.gui.guide.GuideRenderer;
import dev.isxander.controlify.gui.guide.PrecomputedLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuideRenderer.class, remap = false)
public abstract class FeatureNativeGuideProbeMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), remap = false)
    private static void begin(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide, Minecraft minecraft,
                              boolean bottom, boolean contrast, int scale, CallbackInfo callback) {
        QaNativeGuideProbe.begin(graphics, guide, bottom);
    }
    @Inject(method = "extractLines", at = @At("HEAD"), remap = false)
    private static void column(GuiGraphicsExtractor graphics, PrecomputedLines lines, Font font, int width, int height,
                               boolean bottom, boolean right, boolean contrast, CallbackInfo callback) {
        QaNativeGuideProbe.column(lines, right);
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false)
    private static void finish(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide, Minecraft minecraft,
                               boolean bottom, boolean contrast, int scale, CallbackInfo callback) {
        QaNativeGuideProbe.finish();
    }
}
