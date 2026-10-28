package com.salts_inventory_update.compat.alloyforgery.mixin;

import com.salts_inventory_update.compat.alloyforgery.AlloyPropertyBridge;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ServerPlayNetworking.class,remap=false)
abstract class AlloyForgePropertyNetworkingMixin {
    @Inject(method="send",at=@At("HEAD"),cancellable=true)
    private static void salts$scopedProperties(ServerPlayer player,CustomPacketPayload payload,CallbackInfo ci){
        if(AlloyPropertyBridge.send(player,payload))ci.cancel();
    }
}
