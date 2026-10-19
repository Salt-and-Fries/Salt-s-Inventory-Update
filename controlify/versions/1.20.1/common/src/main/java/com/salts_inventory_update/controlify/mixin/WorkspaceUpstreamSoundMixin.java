package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.WorkspaceUiSounds;
import com.salts_inventory_update.controlify.input.WorkspaceContainerScreen;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the companion one audio owner while its screen owns the input; all other screens retain upstream sounds. */
@Mixin(ScreenProcessor.class)
public abstract class WorkspaceUpstreamSoundMixin {
    @Inject(method = "playClackSound", at = @At("HEAD"), cancellable = true, remap = false)
    private static void saltsControlify$ownedClose(CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof WorkspaceContainerScreen) callback.cancel();
    }

}
