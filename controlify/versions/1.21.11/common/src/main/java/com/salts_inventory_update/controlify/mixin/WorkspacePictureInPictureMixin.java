package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.render.WorkspacePictureInPicture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Native PIP submission uses raw bounds; regular GUI vertices already inherit the pose. */
@Mixin(GuiGraphics.class)
public abstract class WorkspacePictureInPictureMixin {
    @Shadow public abstract Matrix3x2fStack pose();

    @ModifyArg(method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState", "submitSignRenderState", "submitProfilerChartRenderState"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitPicturesInPictureState(Lnet/minecraft/client/gui/render/state/pip/PictureInPictureRenderState;)V"), index = 0)
    private PictureInPictureRenderState saltsTransformPreview(PictureInPictureRenderState state) {
        return WorkspacePictureInPicture.transform(state, pose());
    }
}
