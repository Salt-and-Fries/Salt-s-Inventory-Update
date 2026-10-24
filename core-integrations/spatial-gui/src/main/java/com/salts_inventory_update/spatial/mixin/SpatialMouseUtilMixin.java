package com.salts_inventory_update.spatial.mixin;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;
import com.salts_inventory_update.spatial.SpatialDesktopCanvas;
import org.spongepowered.asm.mixin.Shadow;
import org.joml.Vector2d;
import org.tastytrash.spatialGUI.util.RenderUtil;
import com.mojang.blaze3d.pipeline.TextureTarget;

@Mixin(value = MouseHandlerUtil.class, remap = false)
public class SpatialMouseUtilMixin {
    @Shadow private static double mappedX;
    @Shadow private static double mappedY;
    @Inject(method="mapMousePosition",at=@At("RETURN"),cancellable=true)
    private static void salts$logicalCanvasPointer(double x,double y,RenderUtil.QuadBasis quad,RenderUtil.CylinderBasis curve,double guiScale,TextureTarget target,CallbackInfoReturnable<Vector2d> cir) {
        if(SpatialGuiIntegration.active()&&cir.getReturnValue()!=null) {
            double scale=SpatialDesktopCanvas.get().scale();mappedX/=scale;mappedY/=scale;
            cir.setReturnValue(new Vector2d(cir.getReturnValue()).div(scale));
        }
    }
    @Inject(method = "syncCursorGrab", at = @At("HEAD"), cancellable = true)
    private static void salts$keepGameplayGrab(CallbackInfo ci) {
        if (SpatialGuiIntegration.movement()) ci.cancel();
    }
    @Inject(method = "updateMouseGrabForFirstPerson", at = @At("HEAD"), cancellable = true)
    private static void salts$gameplayGrabOwner(boolean firstPerson, CallbackInfo ci) {
        if (SpatialGuiIntegration.movement()) ci.cancel();
    }
    @Inject(method = {"getHoverX", "getHoverY"}, at = @At("HEAD"), cancellable = true)
    private static void salts$noEdgeHover(CallbackInfoReturnable<Integer> cir) {
        if (SpatialGuiIntegration.gameplay() || SpatialGuiIntegration.outsidePanel()) cir.setReturnValue(-10000);
    }
}
