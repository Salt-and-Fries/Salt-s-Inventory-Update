package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.render.WorkspacePictureInPicture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Native PIP submission uses raw bounds; regular GUI vertices already inherit the pose. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class WorkspacePictureInPictureMixin {
    @Shadow public abstract Matrix3x2fStack pose();

    @ModifyArg(method = {"entity", "skin", "book", "bannerPattern", "sign", "profilerChart"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;)V"), index = 0)
    private PictureInPictureRenderState saltsTransformPreview(PictureInPictureRenderState state) {
        return WorkspacePictureInPicture.transform(state, pose());
    }
}
