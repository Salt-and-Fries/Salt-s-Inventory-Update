package com.salts_inventory_update.compat.sophisticated.client;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetMemorySlotPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataPayload;
import net.p3pp3rf1y.sophisticatedcore.network.TransferFullSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.network.TransferItemsPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickPayload;

public final class SophisticatedCoreClientPackets {
    private SophisticatedCoreClientPackets() {
    }

    public static boolean send(DesktopInputContext<?, ?> context, CustomPacketPayload payload) {
        if (payload instanceof SyncContainerClientDataPayload value) {
            return context.sendPayload(value.type().id(), value, SyncContainerClientDataPayload.STREAM_CODEC);
        }
        if (payload instanceof TransferItemsPayload value) {
            return context.sendPayload(value.type().id(), value, TransferItemsPayload.STREAM_CODEC);
        }
        if (payload instanceof TransferFullSlotPayload value) {
            return context.sendPayload(value.type().id(), value, TransferFullSlotPayload.STREAM_CODEC);
        }
        if (payload instanceof SetGhostSlotPayload value) {
            return context.sendPayload(value.type().id(), value, SetGhostSlotPayload.STREAM_CODEC);
        }
        if (payload instanceof SetMemorySlotPayload value) {
            return context.sendPayload(value.type().id(), value, SetMemorySlotPayload.STREAM_CODEC);
        }
        if (payload instanceof TankClickPayload value) {
            return context.sendPayload(value.type().id(), value, TankClickPayload.STREAM_CODEC);
        }
        return false;
    }
}
