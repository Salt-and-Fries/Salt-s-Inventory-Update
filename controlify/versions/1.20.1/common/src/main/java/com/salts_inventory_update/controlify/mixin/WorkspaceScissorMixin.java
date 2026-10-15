package com.salts_inventory_update.controlify.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.salts_inventory_update.controlify.render.AffineScissor;
import com.salts_inventory_update.controlify.render.WorkspaceRenderTransform;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Minecraft 1.21.1's scissor stack ignores the current GUI pose transform. */
@Mixin(GuiGraphics.class)
public abstract class WorkspaceScissorMixin {
    @Shadow
    public abstract PoseStack pose();

    @ModifyArgs(method = "enableScissor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;<init>(IIII)V"))
    private void saltsControlify$transformWindowClip(Args args) {
        if (!WorkspaceRenderTransform.active()) return;
        Matrix4f matrix = pose().last().pose();
        AffineScissor bounds = AffineScissor.transform(args.get(0), args.get(1), args.get(2), args.get(3),
                matrix.m00(), matrix.m10(), matrix.m30(), matrix.m01(), matrix.m11(), matrix.m31());
        args.set(0, bounds.x());
        args.set(1, bounds.y());
        args.set(2, bounds.width());
        args.set(3, bounds.height());
    }
}
