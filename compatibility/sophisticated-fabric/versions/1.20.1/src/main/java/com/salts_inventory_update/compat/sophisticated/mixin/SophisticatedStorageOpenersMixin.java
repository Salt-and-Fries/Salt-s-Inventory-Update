package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.function.Predicate;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps physical Sophisticated Storage blocks open while their menu is hosted in a Salt window. */
@Mixin(ContainerOpenersCounter.class)
public abstract class SophisticatedStorageOpenersMixin {
    @ModifyArg(
        method = "getOpenCount",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
        ),
        index = 2
    )
    private Predicate<? super Player> salts_inventory_update$includeDesktopSessions(
        Predicate<? super Player> nativeOwnership
    ) {
        ContainerOpenersCounter counter = (ContainerOpenersCounter) (Object) this;
        if (!(counter.getClass().getName().startsWith("net.p3pp3rf1y.sophisticatedstorage.block."))) {
            return nativeOwnership;
        }
        return player -> nativeOwnership.test(player)
            || DesktopSessions.hasOpenSessionMatching(
                player,
                menu -> salts_inventory_update$isOwnedBy(nativeOwnership, player, menu)
            );
    }

    private static boolean salts_inventory_update$isOwnedBy(
        Predicate<? super Player> nativeOwnership,
        Player player,
        AbstractContainerMenu desktopMenu
    ) {
        AbstractContainerMenu previousMenu = player.containerMenu;
        player.containerMenu = desktopMenu;
        try {
            return nativeOwnership.test(player);
        } finally {
            player.containerMenu = previousMenu;
        }
    }
}
