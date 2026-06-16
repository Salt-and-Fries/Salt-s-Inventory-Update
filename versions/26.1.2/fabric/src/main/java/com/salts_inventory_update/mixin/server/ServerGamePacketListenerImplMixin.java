package com.salts_inventory_update.mixin.server;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void salts_inventory_update$captureDesktopUseTarget(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        DesktopContainerSessions.captureUseTarget(this.player, packet.getHitResult());
    }

    @Inject(method = "handleUseItemOn", at = @At("RETURN"))
    private void salts_inventory_update$clearDesktopUseTarget(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        DesktopContainerSessions.clearUseTarget(this.player);
    }
}
