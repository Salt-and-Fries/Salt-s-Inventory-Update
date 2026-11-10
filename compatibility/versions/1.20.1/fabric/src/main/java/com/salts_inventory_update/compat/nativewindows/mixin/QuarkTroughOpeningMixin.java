package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** This native overload wraps an empty writer; the trough has no opening bytes. */
@Pseudo
@Mixin(targets="net.minecraftforge.network.NetworkHooks",priority=1100,remap=false)
public abstract class QuarkTroughOpeningMixin {
    @Inject(method="openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;)V",at=@At("HEAD"),cancellable=true)
    private static void open(ServerPlayer player,MenuProvider provider,CallbackInfo ci) {
        if(provider!=null&&provider.getClass().getName().equals("org.violetmoon.quark.content.automation.block.be.FeedingTroughBlockEntity")&&DesktopSessions.shouldCapture(player)
            &&DesktopSessions.openMenuSession(player,provider,null)!=null)ci.cancel();
    }
}
