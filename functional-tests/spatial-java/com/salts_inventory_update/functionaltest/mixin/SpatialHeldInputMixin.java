package com.salts_inventory_update.functionaltest.mixin;
import com.salts_inventory_update.functionaltest.InWorldGameplayHarness;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** QA-only polling seam. All production control state, camera, mouse grab and rendering remain real. */
@Mixin(InputConstants.class)
public class SpatialHeldInputMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void salts$heldFixture(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key == InWorldGameplayHarness.spatialHeldInputKey) cir.setReturnValue(true);
    }
}
