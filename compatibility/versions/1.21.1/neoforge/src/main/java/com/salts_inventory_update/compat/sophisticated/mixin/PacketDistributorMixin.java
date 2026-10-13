package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.server.SophisticatedServerPayloadBridge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

@Mixin(value = PacketDistributor.class, remap = false)
public abstract class PacketDistributorMixin {
    @Inject(method = "sendToPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void salts_inventory_update$routeDetachedSophisticatedPayload(
        ServerPlayer player,
        CustomPacketPayload payload,
        CustomPacketPayload[] additional,
        CallbackInfo ci
    ) {
        if (SophisticatedServerPayloadBridge.capture(player, payload, additional)) {
            ci.cancel();
        }
    }
}
