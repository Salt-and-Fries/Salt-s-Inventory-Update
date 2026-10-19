package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaHoverPacketProbe;
import com.salts_inventory_update.server.DesktopContainerSessions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes authentic integrated-server cleanup without changing it. */
@Mixin(value=DesktopContainerSessions.class,remap=false)
public abstract class FeatureHoverServerProbeMixin {
    @Inject(method="endRecipeHover",at=@At("HEAD"),remap=false)
    private static void end(ServerPlayer player,@Coerce Object sessions,CallbackInfo callback) {
        QaHoverPacketProbe.serverHoverEnd(player,sessions);
    }
    @Inject(method="interruptRecipeHover",at=@At("RETURN"),remap=false)
    private static void returned(ServerPlayer player,CustomPacketPayload payload,Runnable mutation,CallbackInfo callback) {
        QaHoverPacketProbe.serverInterruptionReturned(player,payload);
    }
}
