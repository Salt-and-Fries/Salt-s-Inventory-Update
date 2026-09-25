package com.salts_inventory_update.compat.sophisticated.client;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SyncClientInfoPayload;

public final class SophisticatedBackpackClientState {
    private SophisticatedBackpackClientState() {
    }

    public static void apply(Minecraft minecraft, AbstractContainerMenu menu, ResourceLocation channel, byte[] data) {
        if (!channel.equals(SyncClientInfoPayload.TYPE.id()) || !(menu instanceof BackpackContainer backpackContainer) || minecraft.player == null) {
            return;
        }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), minecraft.player.registryAccess());
        try {
            SyncClientInfoPayload payload = SyncClientInfoPayload.STREAM_CODEC.decode(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing backpack client info data: " + buffer.readableBytes());
            }
            if (payload.renderInfoNbt() == null) {
                return;
            }
            if (payload.slotIndex() >= 0) {
                ItemStack backpack = minecraft.player.getInventory().getItem(payload.slotIndex());
                IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
                wrapper.getRenderInfo().deserializeFrom(payload.renderInfoNbt());
                wrapper.setColumnsTaken(payload.columnsTaken(), false);
            }
            if (backpackContainer.canApplyClientInfo(payload.slotIndex())) {
                backpackContainer.syncClientInfo(payload.renderInfoNbt(), payload.columnsTaken());
            }
        } finally {
            buffer.release();
        }
    }
}
