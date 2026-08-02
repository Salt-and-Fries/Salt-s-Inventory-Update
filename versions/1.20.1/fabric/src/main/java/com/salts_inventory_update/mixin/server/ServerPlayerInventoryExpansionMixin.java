package com.salts_inventory_update.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerInventoryExpansionMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void salts_inventory_update$closeDesktopSessionsBeforeDeath(DamageSource source, CallbackInfo ci) {
        DesktopContainerSessions.playerDying((ServerPlayer) (Object) this);
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void salts_inventory_update$prepareExpansionInventoryRestore(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean keepContents = keepEverything
            || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
            || oldPlayer.isSpectator();
        DesktopContainerSessions.preparePlayerRestore(oldPlayer, keepContents);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void salts_inventory_update$restoreExpansionInventory(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean keepContents = keepEverything
            || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
            || oldPlayer.isSpectator();
        DesktopContainerSessions.playerRestored(oldPlayer, player);
        InventoryExpansion.copyFrom(player, oldPlayer, keepContents);
        InventoryExpansion.syncToClient(player);
    }
}
