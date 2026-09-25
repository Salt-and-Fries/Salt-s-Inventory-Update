package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.p3pp3rf1y.sophisticatedstorage.block.SophisticatedOpenersCounter;

/** Keeps physical Sophisticated Storage blocks open while their menu is hosted in a Salt window. */
@Mixin(Player.class)
public abstract class SophisticatedStorageOpenersMixin {
    @Inject(
        method = "hasContainerOpen(Lnet/minecraft/world/level/block/entity/ContainerOpenersCounter;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void salts_inventory_update$desktopSessionOwnsSophisticatedStorage(
        ContainerOpenersCounter counter,
        BlockPos blockPos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(counter instanceof SophisticatedOpenersCounter)) {
            return;
        }

        Player player = (Player) (Object) this;
        if (DesktopContainerSessions.hasOpenSessionMatching(
            player,
            menu -> salts_inventory_update$isOwnedBy(counter, player, menu)
        )) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean salts_inventory_update$isOwnedBy(
        ContainerOpenersCounter counter,
        Player player,
        AbstractContainerMenu desktopMenu
    ) {
        AbstractContainerMenu previousMenu = player.containerMenu;
        player.containerMenu = desktopMenu;
        try {
            // Delegate to Sophisticated Storage rather than duplicating its identity rules. This
            // covers normal/limited barrels, single/double chests, settings, and future storage
            // variants that use the same opener counter.
            return counter.isOwnContainer(player);
        } finally {
            player.containerMenu = previousMenu;
        }
    }
}
