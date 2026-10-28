package com.salts_inventory_update.compat.farmersdelight.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve Refabricated's native opening bytes through the supported session API. */
@Pseudo
@Mixin(targets="io.github.fabricators_of_create.porting_lib.util.NetworkHooks",remap=false)
abstract class FarmersDelightOpeningMixin {
    @Inject(method={
        "openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        "openScreen(Lnet/minecraft/class_3222;Lnet/minecraft/class_3908;Ljava/util/function/Consumer;)V"
    },at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private static void salts$openCookingPot(ServerPlayer player,MenuProvider provider,
            Consumer<FriendlyByteBuf> openingData,CallbackInfo callback) {
        if(!provider.getClass().getName().startsWith("vectorwing.farmersdelight."))return;
        if(!DesktopSessions.shouldCapture(player))return;
        if(DesktopSessions.openMenuSession(player,provider,openingData)!=null)callback.cancel();
    }
}
