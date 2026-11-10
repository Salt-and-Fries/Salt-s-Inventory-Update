package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.travelersbackpack.TravelersPackets;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.PacketSendListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only an active Traveler widget scope may reroute its allow-listed packets. */
@Mixin(Connection.class)
public abstract class TravelersPacketSenderMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",at=@At("HEAD"),cancellable=true)
    private void salts$hostedAction(Packet<?> packet,PacketSendListener listener,boolean flush,CallbackInfo ci){
        if(packet instanceof ServerboundCustomPayloadPacket payload&&TravelersPackets.send(payload.payload()))ci.cancel();
    }
}
