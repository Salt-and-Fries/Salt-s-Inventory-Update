package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.render.WorkspacePictureScaleAccess;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stores the captured per-preview size after the extraction scope has closed. */
@Mixin({GuiBannerResultRenderState.class, GuiProfilerChartRenderState.class})
public abstract class WorkspacePictureScaleMixin implements WorkspacePictureScaleAccess {
    @Unique private float saltsControlifyPreviewScale = 1;

    @Override public void saltsControlifyPreviewScale(float scale) { saltsControlifyPreviewScale = scale; }

    @Inject(method = "scale", at = @At("RETURN"), cancellable = true)
    private void saltsScalePreview(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(callback.getReturnValueF() * saltsControlifyPreviewScale);
    }
}
