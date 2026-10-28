package com.salts_inventory_update.compat.chipped.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NetworkHooks bypasses the ordinary API opener on legacy Forge. */
@Pseudo
@Mixin(targets="earth.terrarium.chipped.common.utils.forge.ModUtilsImpl",remap=false)
abstract class ChippedForgeOpeningMixin {
    @Inject(method="openMenu",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$desktopOpen(ServerPlayer player,BlockPos pos,MenuProvider provider,CallbackInfo ci){
        if(DesktopSessions.shouldCapture(player)
            &&DesktopSessions.openMenuSession(player,provider,buffer->buffer.writeBlockPos(pos))!=null)ci.cancel();
    }
}
