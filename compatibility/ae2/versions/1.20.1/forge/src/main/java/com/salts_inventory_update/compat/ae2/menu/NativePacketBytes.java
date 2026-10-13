package com.salts_inventory_update.compat.ae2.menu;

import appeng.core.sync.BasePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraftforge.network.NetworkDirection;

/** Forge AE2 supplies the payload through its ordinary native packet encoder. */
final class NativePacketBytes {
    private NativePacketBytes() { }
    static FriendlyByteBuf payload(BasePacket packet) {
        return ((ServerboundCustomPayloadPacket) packet.toPacket(NetworkDirection.PLAY_TO_SERVER)).getData();
    }
}
