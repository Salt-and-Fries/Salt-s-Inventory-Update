package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.OptionalInt;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerOpeningDataMixin {
    @Inject(
        method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void salts_inventory_update$openDesktopMenuWithData(
        MenuProvider provider,
        Consumer<RegistryFriendlyByteBuf> openingDataWriter,
        CallbackInfoReturnable<OptionalInt> cir
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!DesktopContainerSessions.shouldCapture(player)) {
            return;
        }
        OptionalInt desktopSession = DesktopContainerSessions.openMenuSession(player, provider, openingDataWriter);
        if (desktopSession != null) {
            cir.setReturnValue(desktopSession);
            cir.cancel();
        } else if (DesktopContainerSessions.hasActiveSessionTransition(player)) {
            cir.setReturnValue(OptionalInt.empty());
            cir.cancel();
        }
    }
}
