package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.travelersbackpack.TravelersWindows;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend upstream's active-menu notification to its detached SIU sessions. */
@Pseudo @Mixin(targets="com.tiviacz.travelersbackpack.inventory.BackpackWrapper",remap=false)
public abstract class TravelersWrapperMenuMixin {
    @Inject(method="requestMenuUpdate()V",at=@At("RETURN"),remap=false)
    private void salts$allMenus(CallbackInfo ci){TravelersWindows.updateDetachedMenus(this,-1);}
    @Inject(method="requestMenuUpdate(I)V",at=@At("RETURN"),remap=false)
    private void salts$upgradePosition(int slot,CallbackInfo ci){TravelersWindows.updateDetachedMenus(this,slot);}
}
