package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.server.SophisticatedServerPayloadBridge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.PacketDistributor;

@Mixin(value = PacketDistributor.class, remap = true)
public abstract class PacketDistributorMixin {
    @Inject(target = @Desc(value = "sendToPlayer", args = {ServerPlayer.class, CustomPacketPayload.class}), at = @At("HEAD"), cancellable = true, remap = true)
    private static void salts_inventory_update$routeDetachedSophisticatedPayload(
        ServerPlayer player,
        CustomPacketPayload payload,
        CallbackInfo ci
    ) {
        if (SophisticatedServerPayloadBridge.capture(player, payload, new CustomPacketPayload[0])) {
            ci.cancel();
        }
    }
}
