package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VirtualMouseHandler.class)
public abstract class FeatureCursorProbeMixin {
    @Inject(method = "renderVirtualMouse", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", shift = At.Shift.AFTER), remap = false)
    private void observeActualCursorScale(GuiGraphicsExtractor graphics, CallbackInfo ci) { QaFeatureProbe.cursor(graphics); }
}
