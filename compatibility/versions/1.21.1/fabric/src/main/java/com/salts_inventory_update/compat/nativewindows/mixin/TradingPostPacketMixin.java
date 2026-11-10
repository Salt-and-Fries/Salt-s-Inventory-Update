package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tradingpost.TradingPostPackets;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"fuzs.tradingpost.network.S2CMerchantDataMessage$1","fuzs.tradingpost.network.S2CBuildOffersMessage$1","fuzs.tradingpost.network.S2CRemoveMerchantsMessage$1"},remap=false)
public abstract class TradingPostPacketMixin {
    @Inject(method="handle",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$owningWindow(@Coerce Object packet,@Coerce Object player,Object game,CallbackInfo ci){
        if(TradingPostPackets.receive(this,packet))ci.cancel();
    }
}
