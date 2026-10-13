package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Radial selection hides the stock cursor image while retaining Controlify's input state. */
@Mixin(VirtualMouseHandler.class)
public abstract class VirtualCursorVisibilityMixin {
    @Inject(method = "renderVirtualMouse", at = @At("HEAD"), cancellable = true, remap = false)
    private void saltsControlify$hideRadialCursor(GuiGraphics graphics, CallbackInfo callback) {
        if (ControlifyInputBridge.suppressVirtualMouseRendering()) callback.cancel();
    }
}
