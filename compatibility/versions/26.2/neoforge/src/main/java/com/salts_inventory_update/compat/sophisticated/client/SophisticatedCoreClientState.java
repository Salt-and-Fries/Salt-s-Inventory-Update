package com.salts_inventory_update.compat.sophisticated.client;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.network.SyncAdditionalSlotInfoPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncEmptySlotIconsPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncSlotChangeErrorPayload;

public final class SophisticatedCoreClientState {
    private SophisticatedCoreClientState() {
    }

    public static void apply(Minecraft minecraft, AbstractContainerMenu menu, Identifier channel, byte[] data) {
        if (channel.equals(SyncEmptySlotIconsPayload.TYPE.id()) && menu instanceof IAdditionalSlotInfoMenu additional) {
            SyncEmptySlotIconsPayload payload = decode(minecraft, data, SyncEmptySlotIconsPayload.STREAM_CODEC);
            additional.updateEmptySlotIcons(payload.emptySlotIcons());
        } else if (channel.equals(SyncAdditionalSlotInfoPayload.TYPE.id()) && menu instanceof IAdditionalSlotInfoMenu additional) {
            SyncAdditionalSlotInfoPayload payload = decode(minecraft, data, SyncAdditionalSlotInfoPayload.STREAM_CODEC);
            additional.updateAdditionalSlotInfo(
                payload.inaccessibleSlots(),
                payload.inaccessibleSlotsWithoutOverlay(),
                payload.slotLimitOverrides(),
                payload.infiniteSlots(),
                payload.slotFilterItems()
            );
        } else if (channel.equals(SyncSlotChangeErrorPayload.TYPE.id()) && menu instanceof StorageContainerMenuBase<?> storage) {
            SyncSlotChangeErrorPayload payload = decode(minecraft, data, SyncSlotChangeErrorPayload.STREAM_CODEC);
            storage.updateSlotChangeError(payload.slotChangeError());
        }
    }

    private static <P> P decode(Minecraft minecraft, byte[] data, StreamCodec<? super RegistryFriendlyByteBuf, P> codec) {
        if (minecraft.player == null) {
            throw new IllegalStateException("Cannot decode Sophisticated state without a local player");
        }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), minecraft.player.registryAccess());
        try {
            P payload = codec.decode(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing Sophisticated payload data: " + buffer.readableBytes());
            }
            return payload;
        } finally {
            buffer.release();
        }
    }
}
