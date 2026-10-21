package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.network.PacketDistributor;

@Mixin(value = PacketDistributor.class, remap = true)
public abstract class ClientPacketDistributorMixin {
    @Inject(method = "sendToServer", at = @At("HEAD"), cancellable = true, remap = true)
    private static void salts_inventory_update$routeDetachedPayload(
        CustomPacketPayload payload,
        CallbackInfo ci
    ) {
        if (SophisticatedHostedScreenBridge.sendToServer(payload, new CustomPacketPayload[0])) {
            ci.cancel();
        }
    }
}
