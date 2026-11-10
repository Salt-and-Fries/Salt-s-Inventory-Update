package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.travelersbackpack.TravelersWindows;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.tiviacz.travelersbackpack.inventory.BackpackWrapper",remap=false)
public abstract class TravelersWrapperUpdateMixin {
    @Inject(method="requestScreenUpdate",at=@At("HEAD"),remap=false)
    private void salts$hostedUpdate(CallbackInfo ci){TravelersWindows.changed(this);}
}
