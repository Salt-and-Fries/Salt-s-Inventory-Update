package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only trace of actual input samples and their native D-pad dispatches. */
@Mixin(value = VirtualMouseHandler.class, priority = 1100)
public abstract class FeatureNavigationProbeMixin {
    @Inject(method = "enableVirtualMouse", at = @At("HEAD"), remap = false)
    private void enableBefore(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("enable-before", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "enableVirtualMouse", at = @At("RETURN"), remap = false)
    private void enableAfter(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("enable-after", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "disableVirtualMouse", at = @At("HEAD"), remap = false)
    private void disableBefore(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("disable-before", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "disableVirtualMouse", at = @At("RETURN"), remap = false)
    private void disableAfter(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("disable-after", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "onScreenChanged", at = @At("HEAD"), remap = false)
    private void screenBefore(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("screen-before", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "onScreenChanged", at = @At("RETURN"), remap = false)
    private void screenAfter(CallbackInfo callback) { QaFeatureProbe.virtualMouseLifecycle("screen-after", (VirtualMouseHandler)(Object)this, ""); }
    @Inject(method = "onInputModeChanged", at = @At("HEAD"), remap = false)
    private void modeBefore(dev.isxander.controlify.InputMode mode, CallbackInfo callback) {
        QaFeatureProbe.virtualMouseLifecycle("mode-before", (VirtualMouseHandler)(Object)this, mode.name());
    }
    @Inject(method = "onInputModeChanged", at = @At("RETURN"), remap = false)
    private void modeAfter(dev.isxander.controlify.InputMode mode, CallbackInfo callback) {
        QaFeatureProbe.virtualMouseLifecycle("mode-after", (VirtualMouseHandler)(Object)this, mode.name());
    }
    @Inject(method = "handleControllerInput", at = @At("HEAD"), remap = false)
    private void inputBefore(ControllerEntity controller, CallbackInfo callback) {
        QaFeatureProbe.navigation("input-before", controller, (VirtualMouseHandler) (Object) this, "");
    }
    @Inject(method = "handleControllerInput", at = @At("RETURN"), remap = false)
    private void inputAfter(ControllerEntity controller, CallbackInfo callback) {
        QaFeatureProbe.navigation("input-after", controller, (VirtualMouseHandler) (Object) this, "");
    }
    @Inject(method = "snapInDirection", at = @At("HEAD"), remap = false)
    private void snapBefore(ScreenDirection direction, CallbackInfo callback) {
        var controller = Controlify.instance().getCurrentController().orElse(null);
        if (controller != null) QaFeatureProbe.navigation("snap-before", controller, (VirtualMouseHandler) (Object) this, direction.name());
    }
    @Inject(method = "snapInDirection", at = @At("RETURN"), remap = false)
    private void snapAfter(ScreenDirection direction, CallbackInfo callback) {
        var controller = Controlify.instance().getCurrentController().orElse(null);
        if (controller != null) QaFeatureProbe.navigation("snap-after", controller, (VirtualMouseHandler) (Object) this, direction.name());
    }
}
