package com.salts_inventory_update.spatial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialPanelPresentation;
import com.salts_inventory_update.spatial.SpatialMovementTransition;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.render.InventoryRenderer;

@Mixin(value=InventoryRenderer.class,remap=false)
public abstract class SpatialPanelMixin {
    @Shadow @Final private static PoseStack WORLD_POSE_STACK;
    @Unique private float salts$fitScale=1;
    @Inject(method="computeScreenPose",at=@At("RETURN"))
    private void salts$fitWholeDesktop(PoseStack matrices,boolean first,CallbackInfoReturnable<?> cir) {
        salts$fitScale=1;
        SpatialMovementTransition.update();
        if(!SpatialMovementTransition.presenting()){SpatialPanelPresentation.reset();return;}
        var mc=net.minecraft.client.Minecraft.getInstance();
        var old=new Matrix4f(WORLD_POSE_STACK.last().pose());
        float animated=old.getScale(new org.joml.Vector3f()).y;
        float reference=Math.max(animated,org.tastytrash.spatialGUI.util.RenderUtil.getScreenTransformConfig(first).scale());
        if(animated<=.00001f||reference<=.00001f)return;
        // Fit the fully open footprint, then retain the upstream opening/closing scale animation.
        var fit=SpatialPanelPresentation.fit(new Matrix4f(old).scale(reference/animated),first,(float)mc.getWindow().getWidth()/Math.max(1,mc.getWindow().getHeight()));
        var fitted=SpatialPanelPresentation.present(new Matrix4f(fit.pose()).scale(animated/reference),animated/reference);
        salts$fitScale=fitted.getScale(new org.joml.Vector3f()).y/animated;
        if(matrices!=null)matrices.mulPose(new Matrix4f(old).invert().mul(fitted));
        WORLD_POSE_STACK.setIdentity();WORLD_POSE_STACK.mulPose(fitted);
    }
    @ModifyArg(method="updateScreenBases",at=@At(value="INVOKE",target="Lorg/tastytrash/spatialGUI/util/RenderUtil;computeQuadBasis(Lorg/joml/Matrix4f;FF)Lorg/tastytrash/spatialGUI/util/RenderUtil$QuadBasis;"),index=2)
    private float salts$matchingFlatHitSurface(float scale) { return scale*salts$fitScale; }
    @ModifyArg(method="updateScreenBases",at=@At(value="INVOKE",target="Lorg/tastytrash/spatialGUI/util/RenderUtil;computeCylinderBasis(Lorg/joml/Matrix4f;FFF)Lorg/tastytrash/spatialGUI/util/RenderUtil$CylinderBasis;"),index=2)
    private float salts$matchingCurvedHitSurface(float scale) { return scale*salts$fitScale; }
    @ModifyExpressionValue(method={"isCurvedScreenActive","updateScreenBases","addScreenQuadMesh"},at=@At(value="FIELD",target="Lorg/tastytrash/spatialGUI/client/SpatialGUIConfig;curvedScreenArcDegrees:I"))
    private static int salts$flattenForNormalDesktop(int original) {
        return SpatialMovementTransition.presenting()?(int)Math.round(SpatialPanelPresentation.curveDegrees(original)):original;
    }
}
