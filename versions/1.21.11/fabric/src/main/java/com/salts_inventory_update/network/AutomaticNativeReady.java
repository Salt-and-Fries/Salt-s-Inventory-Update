package com.salts_inventory_update.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Optional, separately negotiated native-host feature; ordinary API payloads are unchanged. */
public record AutomaticNativeReady(long nonce, boolean acknowledged) implements CustomPacketPayload {
    public static final Type<AutomaticNativeReady> TYPE=new Type<>(DesktopPackets.id("automatic_native_ready"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AutomaticNativeReady> CODEC=StreamCodec.of(
        (buffer,payload)->{buffer.writeLong(payload.nonce());buffer.writeBoolean(payload.acknowledged());},buffer->new AutomaticNativeReady(buffer.readLong(),buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
