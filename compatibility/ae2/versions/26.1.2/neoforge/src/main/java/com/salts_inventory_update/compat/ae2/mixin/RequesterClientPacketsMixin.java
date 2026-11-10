package com.salts_inventory_update.compat.ae2.mixin;

import com.salts_inventory_update.compat.ae2.nativeview.RequesterDesktop;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes only an active, owned Requester window's direct update record. */
@Mixin(value=ClientPacketDistributor.class,remap=false)
public abstract class RequesterClientPacketsMixin {
    @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void requesterUpdate(CustomPacketPayload packet,CustomPacketPayload[] additional,CallbackInfo ci){
        if(additional.length==0&&RequesterDesktop.routePacket(packet))ci.cancel();
    }
}
