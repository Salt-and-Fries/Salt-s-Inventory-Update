package com.salts_inventory_update.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Optional, separately negotiated native-host feature; ordinary API payloads are unchanged. */
public record AutomaticNativeFallback(long nonce, int sessionId, long token) implements CustomPacketPayload {
    public static final Type<AutomaticNativeFallback> TYPE=new Type<>(DesktopPackets.id("automatic_native_fallback"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AutomaticNativeFallback> CODEC=StreamCodec.of(
        (buffer,payload)->{buffer.writeLong(payload.nonce());buffer.writeVarInt(payload.sessionId());buffer.writeLong(payload.token());},buffer->new AutomaticNativeFallback(buffer.readLong(),buffer.readVarInt(),buffer.readLong()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
