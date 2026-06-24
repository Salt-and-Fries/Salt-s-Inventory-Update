package com.salts_inventory_update.mixin.server;

import java.util.OptionalInt;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (DesktopContainerSessions.shouldCapture(player)) {
            OptionalInt desktopSession = DesktopContainerSessions.openMenuSession(player, provider);
            if (desktopSession != null) {
                cir.setReturnValue(desktopSession);
                cir.cancel();
            }
        }
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopHorseInventory(AbstractHorse horse, Container container, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        boolean diagnostic = isCamelOrLlama(horse);
        if (diagnostic) {
            mountDiag(
                "server_mixin_openHorse_start player={} entityId={} entityType={} entityClass={} camel={} llama={} containerClass={} containerSize={}",
                player.getName().getString(),
                horse.getId(),
                BuiltInRegistries.ENTITY_TYPE.getKey(horse.getType()),
                horse.getClass().getName(),
                horse instanceof Camel,
                horse instanceof Llama,
                container.getClass().getName(),
                container.getContainerSize()
            );
        }
        boolean shouldCapture = DesktopContainerSessions.shouldCapture(player);
        if (diagnostic) {
            mountDiag(
                "server_mixin_openHorse_decision player={} entityId={} shouldCapture={}",
                player.getName().getString(),
                horse.getId(),
                shouldCapture
            );
        }
        if (shouldCapture) {
            DesktopContainerSessions.openHorseSession(player, horse, container);
            ci.cancel();
            if (diagnostic) {
                mountDiag("server_mixin_openHorse_cancelled_vanilla player={} entityId={}", player.getName().getString(), horse.getId());
            }
        } else if (diagnostic) {
            mountDiag("server_mixin_openHorse_vanilla_fallback player={} entityId={}", player.getName().getString(), horse.getId());
        }
    }

    @Inject(method = "openNautilusInventory", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopNautilusInventory(AbstractNautilus nautilus, Container container, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (DesktopContainerSessions.shouldCapture(player)) {
            DesktopContainerSessions.openNautilusSession(player, nautilus, container);
            ci.cancel();
        }
    }

    @Inject(method = "sendMerchantOffers", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sendDesktopMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (DesktopContainerSessions.sendMerchantOffers(player, containerId, offers, villagerLevel, villagerXp, showProgress, canRestock)) {
            ci.cancel();
        }
    }

    private static boolean isCamelOrLlama(AbstractHorse horse) {
        return horse instanceof Camel || horse instanceof Llama;
    }

    private static void mountDiag(String message, Object... args) {
        DesktopDebug.warn("SIU_MOUNT_DIAG " + message, args);
    }
}
