package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.create.CreateNativeAccess;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only an explicitly hosted instance opts in; native-only screens are untouched. */
@Pseudo
@Mixin(targets="com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen",remap=false)
public abstract class CreateStockHeightMixin {
    @ModifyVariable(method={"init","method_25426","m_7856_"},at=@At("STORE"),index=1,require=0,remap=false)
    private int salts$compactNativeViewport(int value) { return CreateNativeAccess.compactHeight(this,value); }
    @Inject(method={"removed","method_25432","m_7861_"},at=@At("HEAD"),require=1,remap=false)
    private void salts$saveStockSettings(CallbackInfo ci){CreateNativeAccess.flushStockClose(this);}
    @WrapWithCondition(method={"removed","method_25432","m_7861_"},at=@At(value="INVOKE",target="Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"),require=1,remap=false)
    private boolean salts$combinedStockSettings(@Coerce Object network,net.minecraft.network.protocol.common.custom.CustomPacketPayload payload){
        return !CreateNativeAccess.suppressStockClose(this);
    }
    @Inject(method={"removed","method_25432","m_7861_"},at=@At("RETURN"),require=1,remap=false)
    private void salts$finishStockSettings(CallbackInfo ci){CreateNativeAccess.finishStockClose(this);}
}
