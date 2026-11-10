package com.salts_inventory_update.mixin.server;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Unique
    private static int salts_inventory_update$useItemProbeLogs;

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void salts_inventory_update$captureDesktopUseTarget(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (salts_inventory_update$useItemProbeLogs < 24) {
            salts_inventory_update$useItemProbeLogs++;
            DesktopDebug.probe(
                "mixin ServerGamePacketListenerImpl.handleUseItemOn player={} hit={} hand={}",
                this.player.getName().getString(),
                packet.getHitResult().getBlockPos(),
                packet.getHand()
            );
        }
    }


    @org.spongepowered.asm.mixin.injection.Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private net.minecraft.world.InteractionResult salts_inventory_update$scopedBlockTarget(
        net.minecraft.server.level.ServerPlayerGameMode mode, ServerPlayer player, net.minecraft.world.level.Level level,
        net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return DesktopContainerSessions.withUseTarget(player, hit, () -> mode.useItemOn(player, level, stack, hand, hit));
    }
}
