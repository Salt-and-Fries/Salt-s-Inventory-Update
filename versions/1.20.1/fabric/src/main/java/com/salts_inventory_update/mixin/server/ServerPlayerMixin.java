package com.salts_inventory_update.mixin.server;

import java.util.OptionalInt;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique
    private static int salts_inventory_update$openMenuProbeLogs;
    @Unique
    private static int salts_inventory_update$horseProbeLogs;
    @Unique
    private static int salts_inventory_update$merchantProbeLogs;

    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean shouldCapture = DesktopContainerSessions.shouldCapture(player);
        if (salts_inventory_update$openMenuProbeLogs < 24) {
            salts_inventory_update$openMenuProbeLogs++;
            DesktopDebug.probe(
                "mixin ServerPlayer.openMenu player={} provider={} shouldCapture={} class={} menuSlots={}",
                player.getName().getString(),
                provider == null ? "null" : provider.getClass().getName(),
                shouldCapture,
                player.getClass().getName(),
                player.inventoryMenu == null ? -1 : player.inventoryMenu.slots.size()
            );
        }
        if (shouldCapture) {
            OptionalInt desktopSession = DesktopContainerSessions.openMenuSession(player, provider);
            if (desktopSession != null) {
                DesktopDebug.probe("mixin ServerPlayer.openMenu captured player={} session={}", player.getName().getString(), desktopSession);
                cir.setReturnValue(desktopSession);
                cir.cancel();
            } else {
                DesktopDebug.probe("mixin ServerPlayer.openMenu not captured player={} reason=openMenuSession-null", player.getName().getString());
            }
        }
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopHorseInventory(AbstractHorse horse, Container container, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean shouldCapture = DesktopContainerSessions.shouldCapture(player);
        if (salts_inventory_update$horseProbeLogs < 12) {
            salts_inventory_update$horseProbeLogs++;
            DesktopDebug.probe(
                "mixin ServerPlayer.openHorseInventory player={} horse={} container={} shouldCapture={}",
                player.getName().getString(),
                horse == null ? "null" : horse.getClass().getName(),
                container == null ? "null" : container.getClass().getName(),
                shouldCapture
            );
        }
        if (shouldCapture) {
            DesktopContainerSessions.openHorseSession(player, horse, container);
            ci.cancel();
        }
    }

    @Inject(method = "sendMerchantOffers", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sendDesktopMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (salts_inventory_update$merchantProbeLogs < 12) {
            salts_inventory_update$merchantProbeLogs++;
            DesktopDebug.probe(
                "mixin ServerPlayer.sendMerchantOffers player={} container={} offers={} level={} xp={}",
                player.getName().getString(),
                containerId,
                offers == null ? -1 : offers.size(),
                villagerLevel,
                villagerXp
            );
        }
        if (DesktopContainerSessions.sendMerchantOffers(player, containerId, offers, villagerLevel, villagerXp, showProgress, canRestock)) {
            ci.cancel();
        }
    }
}
