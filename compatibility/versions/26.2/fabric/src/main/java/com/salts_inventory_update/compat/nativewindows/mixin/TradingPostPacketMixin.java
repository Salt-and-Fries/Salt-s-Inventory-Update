package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tradingpost.TradingPostPackets;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"fuzs.tradingpost.common.network.ClientboundMerchantDataMessage$1","fuzs.tradingpost.common.network.ClientboundBuildOffersMessage$1","fuzs.tradingpost.common.network.ClientboundRemoveMerchantsMessage$1"},remap=false)
public abstract class TradingPostPacketMixin {
    @Inject(method="accept(Lfuzs/puzzleslib/common/api/network/v4/message/play/ClientboundPlayMessage$Context;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$owningWindow(@Coerce Object context,CallbackInfo ci){
        if(TradingPostPackets.receiveV4(this,context))ci.cancel();
    }
}
