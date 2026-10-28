package com.salts_inventory_update.network;

import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import com.salts_inventory_update.protocol.DesktopProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Carries a normal authenticated/known packet with lossless, separately negotiated counts. */
public record AutomaticNativeEnvelope(long nonce, ResourceLocation innerType, byte[] body, int[] counts) implements DesktopPacket {
    public static final ResourceLocation TYPE = DesktopPackets.id("automatic_native_counts");
    public AutomaticNativeEnvelope(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readResourceLocation(), buffer.readByteArray(DesktopProtocol.MAX_ENVELOPE_BYTES), readCounts(buffer));
    }
    public AutomaticNativeEnvelope {
        if (body.length > DesktopProtocol.MAX_ENVELOPE_BYTES || counts.length > DesktopProtocol.MAX_OPEN_SESSION_ITEMS + 1)
            throw new IllegalArgumentException("Native envelope too large");
        body = body.clone(); counts = counts.clone();
        for (int count : counts) if (count < 0) throw new IllegalArgumentException("Negative native stack count");
    }
    private static int[] readCounts(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > DesktopProtocol.MAX_OPEN_SESSION_ITEMS + 1) throw new IllegalArgumentException("Too many native counts");
        int[] result = new int[size]; for (int i = 0; i < size; i++) result[i] = buffer.readVarInt(); return result;
    }
    @Override public ResourceLocation id() { return TYPE; }
    @Override public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(nonce); buffer.writeResourceLocation(innerType); buffer.writeByteArray(body);
        buffer.writeVarInt(counts.length); for (int count : counts) buffer.writeVarInt(count);
    }
}
