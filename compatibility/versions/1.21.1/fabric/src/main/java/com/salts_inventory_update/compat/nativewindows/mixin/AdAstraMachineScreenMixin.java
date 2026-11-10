package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.adastra.AdAstraScreenAccess;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="earth.terrarium.adastra.client.screens.base.MachineScreen",remap=false)
public abstract class AdAstraMachineScreenMixin {
    @Inject(method="createOptionsBar",at=@At("HEAD"),remap=false)
    private void siu$rememberNativeOwner(CallbackInfoReturnable<Object> result){AdAstraScreenAccess.remember(this);}
}
