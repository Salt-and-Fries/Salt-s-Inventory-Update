package com.salts_inventory_update.compat.nativewindows.mixin;

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

/** Both upstream loaders bypass ServerPlayer.openMenu for extended opening data.
 * Preserve the original provider and writer rather than reconstructing a menu. */
@Pseudo
@Mixin(targets={"io.github.fabricators_of_create.porting_lib.util.NetworkHooks","net.minecraftforge.network.NetworkHooks"},remap=false)
public abstract class CreateOpeningMixin {
    @Inject(method={
        "openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        "openScreen(Lnet/minecraft/class_3222;Lnet/minecraft/class_3908;Ljava/util/function/Consumer;)V"
    },at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$openNativeSession(ServerPlayer player,MenuProvider provider,Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer,CallbackInfo ci) {
        if(!DesktopSessions.shouldCapture(player))return;
        var opened=DesktopSessions.openMenuSession(player,provider,writer);
        if(opened!=null)ci.cancel();
    }
}
