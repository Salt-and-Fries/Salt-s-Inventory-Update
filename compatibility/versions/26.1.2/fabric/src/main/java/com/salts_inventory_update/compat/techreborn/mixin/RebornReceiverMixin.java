package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Retain the publisher's handler when it registers through Fabric's public API. */
@Pseudo @Mixin(targets="net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking",remap=false)
public abstract class RebornReceiverMixin {
    @Inject(method="registerGlobalReceiver",at=@At("HEAD"),remap=false)
    private static void salts$remember(CustomPacketPayload.Type<?> type,@Coerce Object handler,CallbackInfoReturnable<Boolean> ci){TechRebornWindows.remember(type,handler);}
}
