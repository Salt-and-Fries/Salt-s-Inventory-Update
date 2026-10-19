package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaNativePointerFixture;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repositions the QA cursor before upstream reads hover and actual configured input bindings. */
@Mixin(targets = "dev.isxander.controlify.screenop.compat.vanilla.AbstractContainerScreenProcessor", remap = false)
public abstract class NativePointerFixtureMixin {
    @Inject(method = "handleScreenVMouse", at = @At("HEAD"), remap = false)
    private void prepareQaNativePointer(ControllerEntity controller, VirtualMouseHandler mouse, CallbackInfo callback) {
        QaNativePointerFixture.beforeNativeBindings(controller);
    }
}
