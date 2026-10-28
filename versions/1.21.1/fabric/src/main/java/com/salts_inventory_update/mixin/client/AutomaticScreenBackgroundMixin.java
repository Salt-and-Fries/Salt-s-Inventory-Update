package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class AutomaticScreenBackgroundMixin {
    // Container screens override renderBackground and call the transparent pass
    // directly. Suppress that pass too, before it darkens captured artwork or
    // fills the transparent space around native tabs. Ordinary screens own no scope.
    @Inject(method = {"renderBackground","renderTransparentBackground","renderBlurredBackground"}, at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$noWholeScreenDim(CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
}
