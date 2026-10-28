package com.salts_inventory_update.functionaltest.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** QA-only synthetic Shift key state for exercising the real click path. */
@Mixin(value=InputConstants.class,remap=false)
public abstract class ChippedShiftQaMixin {
    @Inject(method="isKeyDown",at=@At("HEAD"),cancellable=true,remap=false)
    private static void chippedQaShift(long window,int key,CallbackInfoReturnable<Boolean> result){
        if(Boolean.getBoolean("siu.chipped.qa.shift") && (key==GLFW.GLFW_KEY_LEFT_SHIFT||key==GLFW.GLFW_KEY_RIGHT_SHIFT))result.setReturnValue(true);
    }
}
