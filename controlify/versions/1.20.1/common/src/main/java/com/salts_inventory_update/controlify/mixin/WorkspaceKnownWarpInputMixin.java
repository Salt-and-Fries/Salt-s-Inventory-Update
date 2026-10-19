package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Controlify.class,remap=false)
public abstract class WorkspaceKnownWarpInputMixin {
    @Inject(method="showCursorTemporarily",at=@At("HEAD"),cancellable=true,remap=false)
    private void saltsControlify$ignoreKnownWarpCursorReveal(CallbackInfo callback) {
        if(NativeCursorWarpGuard.knownWarpCallback())callback.cancel();
    }
    @Inject(method="setInputMode",at=@At("HEAD"),cancellable=true,remap=false)
    private void saltsControlify$ignoreKnownWarpInputSwitch(InputMode mode,CallbackInfoReturnable<Boolean> callback) {
        if(mode==InputMode.KEYBOARD_MOUSE && NativeCursorWarpGuard.knownWarpCallback())callback.setReturnValue(false);
    }
}
