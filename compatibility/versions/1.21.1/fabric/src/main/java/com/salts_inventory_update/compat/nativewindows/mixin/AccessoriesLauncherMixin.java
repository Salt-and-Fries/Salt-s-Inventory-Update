package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.accessories.AccessoriesClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Own-player native button/H use the same attached panel. Crouch-H entity editing stays native. */
@Pseudo @Mixin(targets="io.wispforest.accessories.client.AccessoriesClient",remap=false)
public abstract class AccessoriesLauncherMixin {
    @Inject(method="attemptToOpenScreen(Z)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$attachedAccessories(boolean target,CallbackInfoReturnable<Boolean> ci){if(!target&&AccessoriesClient.openFromNative())ci.setReturnValue(true);}
}
