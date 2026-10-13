package com.salts_inventory_update.compat.sophisticated.server;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SyncClientInfoPayload;

public final class SophisticatedBackpackServerPayloads {
    private SophisticatedBackpackServerPayloads() {
    }

    public static byte[] encode(ServerPlayer player, CustomPacketPayload payload) {
        if (payload instanceof SyncClientInfoPayload value) {
            return SophisticatedServerPayloadBridge.encode(player, value, SyncClientInfoPayload.STREAM_CODEC);
        }
        throw new IllegalArgumentException("Unsupported backpack session payload " + payload.type().id());
    }
}
