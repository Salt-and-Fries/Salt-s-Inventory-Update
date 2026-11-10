package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tomstrading.TomsTradingWindows;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.tom.trading.network.NetworkHandler",remap=false)
public abstract class TomsTradingPopupPacketMixin {
    @Inject(method="sendDataToServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$popupSession(CompoundTag tag,CallbackInfo ci){if(TomsTradingWindows.popupData(tag))ci.cancel();}
}
