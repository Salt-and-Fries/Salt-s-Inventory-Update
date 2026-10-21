package com.salts_inventory_update.compat.sophisticated.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.sophisticated.SophisticatedLegacyMessageBridge;

@Pseudo
@Mixin(targets = {"net.p3pp3rf1y.sophisticatedcore.network.PacketHandler",
    "net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler",
    "net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler"}, remap = true)
public abstract class PacketHandlerLegacyMixin {
    @Inject(method = "sendToServer", at = @At("HEAD"), cancellable = true)
    private static void salts_inventory_update$routeClientMessage(Object message, CallbackInfo ci) {
        if (SophisticatedLegacyMessageBridge.sendToServer(message)) ci.cancel();
    }
    @Inject(method = "sendToClient", at = @At("HEAD"), cancellable = true)
    private static void salts_inventory_update$routeServerMessage(ServerPlayer player, Object message, CallbackInfo ci) {
        if (SophisticatedLegacyMessageBridge.sendToClient(player, message)) ci.cancel();
    }
}
