package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import com.salts_inventory_update.controlify.render.ContextualRadialAssets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Measures actual transformed recipe panels, card surfaces and category/tab frames. */
@Mixin(value = ContextualRadialAssets.class, remap = false)
public abstract class FeatureContextualSurfaceProbeMixin {
    @Inject(method="drawRegion",at=@At("HEAD"),remap=false)
    private static void regionBefore(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
        int x,int y,int width,int height,int sx,int sy,int sw,int sh,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.assetRegionBefore(graphics,element,x,y,width,height,sx,sy,sw,sh,color);
    }
    @Inject(method="drawRegion",at=@At("RETURN"),remap=false)
    private static void regionAfter(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
        int x,int y,int width,int height,int sx,int sy,int sw,int sh,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.assetAfter();
    }
    @Inject(method="drawRingHighlight(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIDD)V",at=@At("HEAD"),remap=false)
    private static void sectorBefore(GuiGraphicsExtractor graphics,int x,int y,double angle,double halfAngle,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.sectorBefore(graphics,x,y,angle,halfAngle);
    }
    @Inject(method="drawRingHighlight(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIDD)V",at=@At("RETURN"),remap=false)
    private static void sectorAfter(GuiGraphicsExtractor graphics,int x,int y,double angle,double halfAngle,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.sectorAfter();
    }
    @Inject(method = "draw", at = @At("HEAD"), remap = false)
    private static void drawBefore(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
        int x,int y,int width,int height,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.assetBefore(graphics,element,x,y,width,height,color);
    }
    @Inject(method = "draw", at = @At("RETURN"), remap = false)
    private static void drawAfter(GuiGraphicsExtractor graphics,ContextualRadialAssets.Element element,
        int x,int y,int width,int height,int color,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.assetAfter();
    }
    @Inject(method = "surface", at = @At("HEAD"), remap = false)
    private static void surface(GuiGraphicsExtractor graphics, ContextualRadialAssets.Element element, int x, int y,
                                int width, int height, int border, int fill, CallbackInfo callback) {
        QaNativeGuideProbe.surface(graphics, x, y, width, height);
    }
}
