package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.WorkspaceOpacityProbe;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records native input ownership at the actual processor edge without changing any action. */
@Mixin(targets = "com.salts_inventory_update.controlify.input.WorkspaceScreenProcessor", remap = false)
public abstract class WorkspaceInputProbeMixin {
    @Inject(method = "onControllerUpdate", at = @At("HEAD"), remap = false)
    private void beforeNativeUpdate(ControllerEntity controller, CallbackInfo callback) {
        WorkspaceOpacityProbe.observeInput("controller-update", controller, (ScreenProcessor<?>) (Object) this);
    }

    @Inject(method = "handleScreenVMouse", at = @At("HEAD"), remap = false)
    private void beforeNativeMouse(ControllerEntity controller, VirtualMouseHandler mouse, CallbackInfo callback) {
        WorkspaceOpacityProbe.observeInput("native-mouse", controller, (ScreenProcessor<?>) (Object) this);
    }
}
