package com.salts_inventory_update.compat.sophisticated.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.SophisticatedLegacyMessageBridge;

@Mixin(value = PacketHandler.class, remap = false)
public abstract class PacketHandlerLegacyMixin {
    @Inject(method = "sendToServer", at = @At("HEAD"), cancellable = true)
    private <M> void salts_inventory_update$routeClientMessage(M message, CallbackInfo ci) {
        if (SophisticatedLegacyMessageBridge.sendToServer((PacketHandler) (Object) this, message)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendToClient", at = @At("HEAD"), cancellable = true)
    private <M> void salts_inventory_update$routeServerMessage(ServerPlayer player, M message, CallbackInfo ci) {
        if (SophisticatedLegacyMessageBridge.sendToClient((PacketHandler) (Object) this, player, message)) {
            ci.cancel();
        }
    }
}
