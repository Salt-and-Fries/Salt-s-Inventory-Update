package com.salts_inventory_update.compat.sophisticated.client;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.network.MobCatcherReleasePayload;

public final class SophisticatedBackpackClientPackets {
    private SophisticatedBackpackClientPackets() {
    }

    public static boolean send(DesktopInputContext<?, ?> context, CustomPacketPayload payload) {
        if (payload instanceof BackpackOpenPayload value) {
            return context.sendPayload(value.type().id(), value, BackpackOpenPayload.STREAM_CODEC);
        }
        if (payload instanceof MobCatcherReleasePayload value) {
            return context.sendPayload(value.type().id(), value, MobCatcherReleasePayload.STREAM_CODEC);
        }
        return false;
    }
}
