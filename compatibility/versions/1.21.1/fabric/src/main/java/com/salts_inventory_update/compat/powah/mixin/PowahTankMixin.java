package com.salts_inventory_update.compat.powah.mixin;
import com.salts_inventory_update.compat.powah.PowahClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="owmii.powah.lib.logistics.inventory.AbstractTileContainer",remap=false)
public abstract class PowahTankMixin {
    @Inject(method="interactWithTank",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$ownedTank(boolean drain,CallbackInfo ci){if(PowahClient.sendTank(this,drain))ci.cancel();}
}
