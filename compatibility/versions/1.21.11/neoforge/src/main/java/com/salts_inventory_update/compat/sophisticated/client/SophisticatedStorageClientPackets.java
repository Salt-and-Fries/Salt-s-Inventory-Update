package com.salts_inventory_update.compat.sophisticated.client;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryPayload;

public final class SophisticatedStorageClientPackets {
    private SophisticatedStorageClientPackets() {
    }

    public static boolean send(DesktopInputContext<?, ?> context, CustomPacketPayload payload) {
        if (payload instanceof OpenStorageInventoryPayload value) {
            return context.sendPayload(value.type().id(), value, OpenStorageInventoryPayload.STREAM_CODEC);
        }
        return false;
    }
}
