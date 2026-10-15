package com.salts_inventory_update.controlify.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.salts_inventory_update.controlify.render.WorkspaceOpacityCaptureAccess;
import com.salts_inventory_update.controlify.render.WorkspaceOpacityVertexConsumer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changes only vertex colors; native pipeline, texture, clipping and PIP types remain intact. */
@Mixin(GuiRenderer.class)
public abstract class WorkspaceOpacityVertexMixin {
    @Shadow @Final private GuiRenderState renderState;
    @Unique private float saltsControlifyOpacity = 1;
    @Unique private boolean saltsControlifyPremultiplied;

    @Inject(method = "addElementToMesh", at = @At("HEAD"))
    private void saltsReadOpacity(GuiElementRenderState state, CallbackInfo ci) {
        saltsControlifyOpacity = ((WorkspaceOpacityCaptureAccess) (Object) renderState).saltsControlifyOpacity(state);
        saltsControlifyPremultiplied = saltsControlifyOpacity < 1 && state.pipeline().getColorTargetState().blendFunction()
                .filter(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA::equals).isPresent();
    }

    @ModifyArg(method = "addElementToMesh",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiElementRenderState;buildVertices(Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"), index = 0)
    private VertexConsumer saltsFadeVertices(VertexConsumer consumer) {
        return saltsControlifyOpacity == 1 ? consumer
                : new WorkspaceOpacityVertexConsumer(consumer, saltsControlifyOpacity, saltsControlifyPremultiplied);
    }
}
