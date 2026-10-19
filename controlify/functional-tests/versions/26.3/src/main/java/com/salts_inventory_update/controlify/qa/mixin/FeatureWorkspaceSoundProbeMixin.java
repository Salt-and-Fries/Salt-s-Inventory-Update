package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.WorkspaceUiSounds;
import com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorkspaceUiSounds.class, remap = false)
public abstract class FeatureWorkspaceSoundProbeMixin {
    @Shadow(remap = false) private static boolean previewing;
    @Shadow(remap = false) private static long generation;
    @Inject(method = "play", at = @At("RETURN"), remap = false)
    private static void request(WorkspaceUiSounds.Action action, CallbackInfoReturnable<Boolean> callback) { QaVisualFeatureProbe.soundRequested(action.name(), callback.getReturnValue()); }
    @Inject(method = "emit", at = @At("HEAD"), remap = false)
    private static void before(WorkspaceUiSounds.Action action, CallbackInfoReturnable<Boolean> callback) { QaVisualFeatureProbe.soundBefore(action.name(), previewing, generation); }
    @Inject(method = "emit", at = @At("RETURN"), remap = false)
    private static void after(WorkspaceUiSounds.Action action, CallbackInfoReturnable<Boolean> callback) { QaVisualFeatureProbe.soundAfter(callback.getReturnValue()); }
}
