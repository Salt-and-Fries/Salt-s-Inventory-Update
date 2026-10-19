package com.salts_inventory_update.controlify.qa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** QA-only isolation of desktop motion before native mixed-input callbacks can compete with the test driver. */
@Mixin(SDLEventHandler.class)
public abstract class FeatureHostMouseIsolationMixin {
    @WrapOperation(method = "pollEvents", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/platform/SDLEventHandler;handleMouseMotionEvent(Lorg/lwjgl/sdl/SDL_Event;)V"))
    private void isolateMotion(SDLEventHandler handler, SDL_Event event, Operation<Void> original) {
        if (ControlifyGameplayHarness.isolateHostMouseMotion()) QaFeatureProbe.isolatedHostMotion(event);
        else original.call(handler, event);
    }
}
