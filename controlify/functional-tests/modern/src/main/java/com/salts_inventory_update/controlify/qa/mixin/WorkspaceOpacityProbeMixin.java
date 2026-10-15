package com.salts_inventory_update.controlify.qa.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.salts_inventory_update.controlify.qa.WorkspaceOpacityProbe;
import com.salts_inventory_update.controlify.render.WorkspaceOpacityVertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only included by the opt-in named-runtime QA mod; never part of production artifacts. */
@Mixin(value = WorkspaceOpacityVertexConsumer.class, remap = false)
public abstract class WorkspaceOpacityProbeMixin {
    @Shadow @Final private float opacity;
    @Shadow @Final private boolean premultiplied;

    @Inject(method = {
            "setColor(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            "setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
    }, at = @At("RETURN"), remap = false)
    private void observeNativeColor(CallbackInfoReturnable<VertexConsumer> callback) {
        WorkspaceOpacityProbe.observe(opacity, premultiplied);
    }
}
