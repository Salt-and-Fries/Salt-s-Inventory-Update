package com.salts_inventory_update.api.server.desktop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerSessionContext<T extends AbstractContainerMenu, S> {
    ServerPlayer player();

    T menu();

    int sessionId();

    String sourceKey();

    boolean visible();

    boolean ghostPinned();

    S state();

    void sendToClient(Identifier channel, byte[] data);

    default <P> void sendToClient(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec) {
        this.sendToClient(channel, com.salts_inventory_update.api.desktop.DesktopPayloadCodecs.encode(this.player().registryAccess(), codec, payload));
    }

    void broadcastChanges();
}
