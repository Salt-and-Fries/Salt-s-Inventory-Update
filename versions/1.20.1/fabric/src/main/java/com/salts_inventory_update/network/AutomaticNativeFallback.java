package com.salts_inventory_update.network;

import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Declines only an authorized automatic session, never an API-created window. */
public record AutomaticNativeFallback(long nonce, int sessionId, long token) implements DesktopPacket {
    public static final ResourceLocation TYPE = DesktopPackets.id("automatic_native_fallback");
    public AutomaticNativeFallback(FriendlyByteBuf buffer) { this(buffer.readLong(), buffer.readVarInt(), buffer.readLong()); }
    @Override public ResourceLocation id() { return TYPE; }
    @Override public void write(FriendlyByteBuf buffer) { buffer.writeLong(nonce); buffer.writeVarInt(sessionId); buffer.writeLong(token); }
}
