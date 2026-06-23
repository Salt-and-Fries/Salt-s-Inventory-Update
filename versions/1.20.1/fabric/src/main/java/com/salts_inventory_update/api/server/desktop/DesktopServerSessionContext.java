package com.salts_inventory_update.api.server.desktop;

import net.minecraft.resources.ResourceLocation;
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

    void sendToClient(ResourceLocation channel, byte[] data);

    void broadcastChanges();
}
