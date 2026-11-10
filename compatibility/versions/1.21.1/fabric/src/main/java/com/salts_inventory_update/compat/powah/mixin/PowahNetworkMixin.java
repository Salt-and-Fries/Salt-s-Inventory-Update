package com.salts_inventory_update.compat.powah.mixin;
import com.salts_inventory_update.compat.powah.PowahClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="owmii.powah.network.Network",remap=false)
public abstract class PowahNetworkMixin {
    @Inject(method="toServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$ownedAction(@Coerce Object message,CallbackInfo ci){if(PowahClient.send(message))ci.cancel();}
}
