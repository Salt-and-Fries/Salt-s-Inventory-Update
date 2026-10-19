package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Radial selection hides the stock cursor image while retaining Controlify's input state. */
@Mixin(VirtualMouseHandler.class)
public abstract class VirtualCursorVisibilityMixin {
    @Inject(method = "renderVirtualMouse", at = @At("HEAD"), cancellable = true, remap = false)
    private void saltsControlify$hideRadialCursor(GuiGraphics graphics, CallbackInfo callback) {
        if (ControlifyInputBridge.suppressVirtualMouseRendering()) callback.cancel();
    }

    @ModifyArgs(method = "renderVirtualMouse", at = @At(value = "INVOKE", target = "Ldev/isxander/controlify/utils/render/CGuiPose;scale(FF)Ldev/isxander/controlify/utils/render/CGuiPose;"), remap = false)
    private void saltsControlify$scaleWindowCursor(Args args) {
        if (!(Minecraft.getInstance().screen instanceof RadialInventoryScreen screen)) return;
        var mouse = Controlify.instance().virtualMouseHandler();
        var window = Minecraft.getInstance().getWindow();
        double x = mouse.getCurrentX(0f) * (double) window.getGuiScaledWidth() / window.getScreenWidth();
        double y = mouse.getCurrentY(0f) * (double) window.getGuiScaledHeight() / window.getScreenHeight();
        float scale = screen.controllerCursorScale(x, y);
        args.set(0, (Float) args.get(0) * scale);
        args.set(1, (Float) args.get(1) * scale);
    }
}
