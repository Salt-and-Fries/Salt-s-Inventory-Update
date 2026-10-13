package com.salts_inventory_update.compat.ae2.menu;

import appeng.core.sync.BasePacket;
import net.minecraft.network.FriendlyByteBuf;

/** Fabric AE2 exposes its own encoded payload directly. */
final class NativePacketBytes {
    private NativePacketBytes() { }
    static FriendlyByteBuf payload(BasePacket packet) { return packet.getPayload(); }
}
