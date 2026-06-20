package com.salts_inventory_update.api.server.desktop;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerPayloadContext<T extends AbstractContainerMenu> {
    ServerPlayer player();

    T menu();

    int sessionId();

    String sourceKey();

    Identifier channel();

    byte[] data();

    void sendToClient(Identifier channel, byte[] data);

    void broadcastChanges();
}
