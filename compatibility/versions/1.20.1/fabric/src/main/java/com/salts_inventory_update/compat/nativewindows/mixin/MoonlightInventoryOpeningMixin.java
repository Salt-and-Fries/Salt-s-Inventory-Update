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

/** Moonlight Fabric opens native menus without calling ServerPlayer.openMenu. */
@Pseudo
@Mixin(targets="net.mehvahdjukaar.moonlight.core.network.fabric.ClientBoundOpenScreenMessage",remap=false)
public abstract class MoonlightInventoryOpeningMixin {
    @Inject(method="openMenu",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts_inventory_compat$inventoryWindow(ServerPlayer player,MenuProvider provider,
            Consumer<FriendlyByteBuf> openingData,CallbackInfo ci) {
        if(!DesktopSessions.shouldCapture(player))return;
        // Core decides eligibility; native-only and unsupported menus keep the
        // original opener. The original opening-data writer stays authoritative.
        var opened=DesktopSessions.openMenuSession(player,provider,openingData);
        if(opened!=null)ci.cancel();
    }
}
