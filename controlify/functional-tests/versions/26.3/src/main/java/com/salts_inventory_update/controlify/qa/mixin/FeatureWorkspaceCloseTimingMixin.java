package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Proves closing finishes in the first processed back edge, independent of host stalls. */
@Mixin(value = SaltsControlifyClient.class, remap = false)
public abstract class FeatureWorkspaceCloseTimingMixin {
    @Inject(method = "closeHighlighted", at = @At("HEAD"), remap = false)
    private void highlightedBefore(CallbackInfo callback) { ControlifyGameplayHarness.observeNativeClose("highlighted", false); }
    @Inject(method = "closeHighlighted", at = @At("RETURN"), remap = false)
    private void highlightedAfter(CallbackInfo callback) { ControlifyGameplayHarness.observeNativeClose("highlighted", true); }
    @Inject(method = "closeWorkspace", at = @At("HEAD"), remap = false)
    private void workspaceBefore(CallbackInfo callback) { ControlifyGameplayHarness.observeNativeClose("workspace", false); }
    @Inject(method = "closeWorkspace", at = @At("RETURN"), remap = false)
    private void workspaceAfter(CallbackInfo callback) { ControlifyGameplayHarness.observeNativeClose("workspace", true); }
}
