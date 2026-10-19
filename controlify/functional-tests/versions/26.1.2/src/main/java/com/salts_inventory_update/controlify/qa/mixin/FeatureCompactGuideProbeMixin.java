package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import dev.isxander.controlify.gui.guide.GuideInstanceImpl;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes compact entries resolved by the actual native Controlify guide instance. */
@Mixin(targets = "com.salts_inventory_update.controlify.WorkspaceActionGuide", remap = false)
public abstract class FeatureCompactGuideProbeMixin {
    @Inject(method = "compactLayout", at = @At("RETURN"), remap = false)
    private static void layout(GuideInstanceImpl<?> guide, int width, int height, int scale,
        CallbackInfoReturnable<com.salts_inventory_update.controlify.model.CompactGuideLayout.Layout> callback) {
        QaNativeGuideProbe.compactLayout(callback.getReturnValue(),width,height);
    }
    @Inject(method = "renderCompact", at = @At("HEAD"), remap = false)
    private static void begin(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide, boolean contrast, int scale, CallbackInfo callback) {
        QaNativeGuideProbe.beginCompact(graphics, guide);
    }
    @Inject(method = "compactEntry", at = @At("HEAD"), remap = false)
    private static void entry(GuiGraphicsExtractor graphics, Font font, Component glyph, Component label,
                              int x, int y, int width, int height, CallbackInfo callback) {
        QaNativeGuideProbe.compactEntry(graphics, font, glyph, label, x, y, width, height);
    }
    @Inject(method = "renderCompact", at = @At("RETURN"), remap = false)
    private static void finish(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide, boolean contrast, int scale, CallbackInfo callback) {
        QaNativeGuideProbe.finish();
    }
}
