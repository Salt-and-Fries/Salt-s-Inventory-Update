package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.graves.UniversalGravesWindows;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(targets="eu.pb4.sgui.api.gui.SimpleGui",remap=false)
public abstract class UniversalGravesGuiMixin {
    @Inject(method="sendGui",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$graveWindow(CallbackInfoReturnable<Boolean> ci){if(UniversalGravesWindows.open(this))ci.setReturnValue(true);}
    @Inject(method="isOpen",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$detachedOpen(CallbackInfoReturnable<Boolean> ci){Boolean open=UniversalGravesWindows.isOpen(this);if(open!=null)ci.setReturnValue(open);}
    @Inject(method="close(Z)V",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$detachedClose(boolean nativeClosed,CallbackInfo ci){if(UniversalGravesWindows.close(this))ci.cancel();}
}
