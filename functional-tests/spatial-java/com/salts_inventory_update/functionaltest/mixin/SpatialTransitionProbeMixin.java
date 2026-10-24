package com.salts_inventory_update.functionaltest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.salts_inventory_update.functionaltest.SpatialTransitionProbe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="org.tastytrash.spatialGUI.render.InventoryRenderer",remap=false)
public class SpatialTransitionProbeMixin {
    @Shadow @Final private static PoseStack WORLD_POSE_STACK;
    @Inject(method="addScreenQuadMesh",at=@At("HEAD"))
    private void salts$measureSubmittedSurface(CallbackInfo ci) {
        SpatialTransitionProbe.observe(WORLD_POSE_STACK.last().pose());
    }
}
