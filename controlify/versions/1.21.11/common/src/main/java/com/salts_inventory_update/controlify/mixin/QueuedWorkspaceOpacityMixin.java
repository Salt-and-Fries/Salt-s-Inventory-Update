package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.render.WorkspaceOpacity;
import com.salts_inventory_update.controlify.render.WorkspaceOpacityCaptureAccess;
import net.minecraft.client.gui.render.state.*;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

/** Retains opacity through native deferred text, item and entity conversions. */
@Mixin(GuiRenderState.class)
public abstract class QueuedWorkspaceOpacityMixin implements WorkspaceOpacityCaptureAccess {
    @Unique private final WorkspaceOpacity.Capture saltsControlifyOpacity = new WorkspaceOpacity.Capture();

    @Inject(method = {"submitGuiElement", "submitGlyphToCurrentLayer"}, at = @At("HEAD"))
    private void saltsCaptureElement(GuiElementRenderState state, CallbackInfo ci) { saltsControlifyOpacity.record(state); }

    @Inject(method = "submitBlitToCurrentLayer", at = @At("HEAD"))
    private void saltsCaptureBlit(BlitRenderState state, CallbackInfo ci) { saltsControlifyOpacity.record(state); }

    @Inject(method = "submitText", at = @At("HEAD"))
    private void saltsCaptureText(GuiTextRenderState state, CallbackInfo ci) { saltsControlifyOpacity.record(state); }

    @Inject(method = "submitItem", at = @At("HEAD"))
    private void saltsCaptureItem(GuiItemRenderState state, CallbackInfo ci) { saltsControlifyOpacity.record(state); }

    @Inject(method = "submitPicturesInPictureState", at = @At("HEAD"))
    private void saltsCapturePicture(PictureInPictureRenderState state, CallbackInfo ci) { saltsControlifyOpacity.record(state); }

    @ModifyVariable(method = {"forEachText", "forEachItem", "forEachPictureInPicture"}, at = @At("HEAD"), argsOnly = true)
    private <T> Consumer<T> saltsRestoreConversionOpacity(Consumer<T> visitor) { return saltsControlifyOpacity.wrap(visitor); }

    @Inject(method = "reset", at = @At("RETURN"))
    private void saltsClearFrameOpacity(CallbackInfo ci) { saltsControlifyOpacity.clear(); }

    @Override public float saltsControlifyOpacity(Object state) { return saltsControlifyOpacity.opacity(state); }
}
