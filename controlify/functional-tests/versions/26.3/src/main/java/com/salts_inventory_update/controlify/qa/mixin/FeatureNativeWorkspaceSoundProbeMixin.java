package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class FeatureNativeWorkspaceSoundProbeMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void nativePlay(SoundInstance sound, CallbackInfoReturnable<Object> callback) { QaVisualFeatureProbe.nativeSound(sound); }
    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void nativeStop(SoundInstance sound, CallbackInfo callback) { QaVisualFeatureProbe.nativeStop(sound); }
}
