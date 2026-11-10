package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.quark.QuarkClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leave the normal inventory opener to SIU only while the authenticated desktop is available. */
@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.module.BackpackModule$Client",remap=false)
public abstract class QuarkBackpackScreenMixin {
    @Inject(method={"onOpenGUI","clientTick"},at=@At("HEAD"),cancellable=true)
    private void pair(@Coerce Object event,CallbackInfo ci) { if(QuarkClient.useInventoryPair())ci.cancel(); }
}
