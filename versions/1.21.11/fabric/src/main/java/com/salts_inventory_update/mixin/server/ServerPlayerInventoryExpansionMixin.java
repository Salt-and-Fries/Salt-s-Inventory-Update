package com.salts_inventory_update.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

import com.salts_inventory_update.inventory.InventoryExpansion;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerInventoryExpansionMixin {
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void salts_inventory_update$restoreExpansionInventory(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean keepContents = keepEverything
            || Boolean.TRUE.equals(player.level().getGameRules().get(GameRules.KEEP_INVENTORY))
            || oldPlayer.isSpectator();
        InventoryExpansion.copyFrom(player, oldPlayer, keepContents);
        InventoryExpansion.syncToClient(player);
    }
}
