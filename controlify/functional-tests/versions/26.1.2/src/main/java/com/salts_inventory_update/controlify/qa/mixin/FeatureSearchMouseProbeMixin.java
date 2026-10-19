package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the real native compatibility clicks before either click changes the active screen. */
@Mixin(MouseHandler.class)
public abstract class FeatureSearchMouseProbeMixin {
    @Inject(method="onButton",at=@At("HEAD"))
    private void observeNativeSearchMouse(long window, MouseButtonInfo button, int action, CallbackInfo callback) {
        ControlifyGameplayHarness.observeNativeSearchMouse(button.button(),action);
    }
}
