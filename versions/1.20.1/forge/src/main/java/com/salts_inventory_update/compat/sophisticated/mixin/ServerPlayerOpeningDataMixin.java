package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

@Mixin(value = NetworkHooks.class, remap = false)
public abstract class ServerPlayerOpeningDataMixin {
    @Inject(
        method = "openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void salts_inventory_update$openDesktopMenuWithData(
        ServerPlayer player,
        MenuProvider provider,
        Consumer<FriendlyByteBuf> openingDataWriter,
        CallbackInfo ci
    ) {
        if (!DesktopContainerSessions.shouldCapture(player)) {
            return;
        }
        if (DesktopContainerSessions.openMenuSession(player, provider, openingDataWriter) != null
            || DesktopContainerSessions.hasActiveSessionTransition(player)) {
            ci.cancel();
        }
    }
}
