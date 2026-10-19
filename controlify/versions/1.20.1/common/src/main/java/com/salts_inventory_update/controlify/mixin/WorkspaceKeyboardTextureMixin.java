package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;
import com.salts_inventory_update.controlify.render.WorkspaceKeyboardRenderType;
import dev.isxander.controlify.utils.render.ExtraRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Controlify 2.0.4 uses an untextured GUI shader for its textured keyboard vertices. */
@Mixin(value=ExtraRenderTypes.class,remap=false)
public abstract class WorkspaceKeyboardTextureMixin {
    @Inject(method="guiTextured",at=@At("HEAD"),cancellable=true,remap=false)
    private static void saltsControlify$nativeKeyboardTexture(ResourceLocation texture,CallbackInfoReturnable<RenderType> callback){
        if(WorkspaceSearchKeyboard.active())callback.setReturnValue(WorkspaceKeyboardRenderType.textured(texture));
    }
}
