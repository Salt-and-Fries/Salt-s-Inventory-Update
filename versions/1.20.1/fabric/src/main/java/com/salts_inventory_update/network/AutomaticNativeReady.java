package com.salts_inventory_update.network;

import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Explicit feature negotiation; older Core/API clients retain the original protocol paths. */
public record AutomaticNativeReady(long nonce, boolean acknowledged) implements DesktopPacket {
    public static final ResourceLocation TYPE = DesktopPackets.id("automatic_native_ready");
    public AutomaticNativeReady(FriendlyByteBuf buffer) { this(buffer.readLong(), buffer.readBoolean()); }
    @Override public ResourceLocation id() { return TYPE; }
    @Override public void write(FriendlyByteBuf buffer) { buffer.writeLong(nonce); buffer.writeBoolean(acknowledged); }
}
