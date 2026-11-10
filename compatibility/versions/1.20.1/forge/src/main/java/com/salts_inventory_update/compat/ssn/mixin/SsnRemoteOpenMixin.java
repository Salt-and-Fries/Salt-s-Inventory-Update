package com.salts_inventory_update.compat.ssn.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Forge's extended opener bypasses ServerPlayer.openMenu; adopt only SSN's validated remote open. */
@Pseudo
@Mixin(targets="com.lothrazar.storagenetwork.util.UtilRemote",remap=false)
public abstract class SsnRemoteOpenMixin {
    @WrapOperation(method="openRemote",at=@At(value="INVOKE",target="Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;)V",remap=false),remap=false)
    private static void salts$remoteWindow(ServerPlayer player,MenuProvider provider,Operation<Void> original){
        if(DesktopSessions.shouldCapture(player)&&DesktopSessions.openMenuSession(player,provider,data->data.writeByte(1))!=null)return;
        original.call(player,provider);
    }
}
