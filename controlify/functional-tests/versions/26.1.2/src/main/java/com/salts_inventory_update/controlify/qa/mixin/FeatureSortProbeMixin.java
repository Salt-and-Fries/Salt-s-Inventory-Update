package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the existing Core send method after it accepts or rejects the actual native sort. */
@Mixin(DesktopContainerClient.class)
public abstract class FeatureSortProbeMixin {
    @Inject(method = "sortWindows", at = @At("RETURN"), remap = false)
    private static void observeSortRequest(int source, List<Integer> destinations, int focused, boolean force, CallbackInfoReturnable<Boolean> ci) {
        QaFeatureProbe.sortRequest(source, destinations, focused, force, ci.getReturnValue());
    }
}
