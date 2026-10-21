package com.salts_inventory_update.functionaltest.mixin;

import com.salts_inventory_update.functionaltest.InWorldGameplayHarness;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** QA-only synthetic Shift state, never included in a production JAR. */
@Mixin(value=InputConstants.class,remap=false)
public abstract class MouseTweaksModernQaInputMixin {
    @Inject(method="isKeyDown(I)Z",at=@At("HEAD"),cancellable=true)
    private static void shift(int key,CallbackInfoReturnable<Boolean> ci){
        if(InWorldGameplayHarness.mouseTweaksShiftHeld() && (key==InputConstants.KEY_LSHIFT || key==InputConstants.KEY_RSHIFT))ci.setReturnValue(true);
    }
}
