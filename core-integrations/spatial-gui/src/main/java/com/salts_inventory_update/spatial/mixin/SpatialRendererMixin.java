package com.salts_inventory_update.spatial.mixin;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialMovementTransition;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(value = SpatialGUIRenderer.class, remap = false)
public class SpatialRendererMixin {
    @Inject(method = "updateMousePosition()V", at = @At("HEAD"), cancellable = true)
    private void salts$keepNativeMovementCoordinates(CallbackInfo ci) {
        // GLFW accumulates movement from raw positions. Replacing them with panel
        // coordinates between callbacks corrupts the next real mouse delta.
        if (SpatialGuiIntegration.gameplay()) ci.cancel();
    }
    @org.spongepowered.asm.mixin.Shadow private net.minecraft.world.phys.Vec3 cameraStartPos;
    @Inject(method = {"shouldCapture", "isClosing"}, at = @At("HEAD"), cancellable = true)
    private void salts$normalCameraPass(CallbackInfoReturnable<Boolean> cir) {
        if (SpatialGuiIntegration.cameraPass) { cameraStartPos = null; cir.setReturnValue(false); }
    }
    @Inject(method = "isCrosshairModeActive", at = @At("HEAD"), cancellable = true)
    private static void salts$gameplayMouse(CallbackInfoReturnable<Boolean> cir) {
        if (SpatialGuiIntegration.gameplay()) cir.setReturnValue(false);
    }
    @ModifyReturnValue(method = "getFovOverrideFactor", at = @At("RETURN"))
    private float salts$gameplayFov(float original) {
        return SpatialMovementTransition.presenting() ? original*(float)(1-SpatialMovementTransition.blend()) : original;
    }
    @Inject(method = "anchoredYaw", at = @At("HEAD"), cancellable = true)
    private void salts$followPlayerYaw(CallbackInfoReturnable<Float> cir) {
        if (SpatialGuiIntegration.active()) cir.setReturnValue(Minecraft.getInstance().player.getYRot());
    }
    @Inject(method = "anchoredPitch", at = @At("HEAD"), cancellable = true)
    private void salts$followPlayerPitch(CallbackInfoReturnable<Float> cir) {
        if (SpatialGuiIntegration.active()) cir.setReturnValue(Minecraft.getInstance().player.getXRot());
    }
    @Inject(method = "updateFrame", at = @At("HEAD"))
    private void salts$refresh(CallbackInfo ci) { SpatialGuiIntegration.refresh(); }
    @Inject(method = "updateFrame", at = @At("TAIL"))
    private void salts$finishFrameUpdate(CallbackInfo ci) { SpatialMovementTransition.update(); }
}
