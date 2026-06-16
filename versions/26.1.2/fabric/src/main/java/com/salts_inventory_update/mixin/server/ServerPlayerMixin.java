package com.salts_inventory_update.mixin.server;

import java.util.OptionalInt;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (DesktopContainerSessions.shouldCapture(player)) {
            cir.setReturnValue(DesktopContainerSessions.openMenuSession(player, provider));
            cir.cancel();
        }
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopHorseInventory(AbstractHorse horse, Container container, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (DesktopContainerSessions.shouldCapture(player)) {
            DesktopContainerSessions.openHorseSession(player, horse, container);
            ci.cancel();
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
}
