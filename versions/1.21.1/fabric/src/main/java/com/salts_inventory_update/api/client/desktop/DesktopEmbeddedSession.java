package com.salts_inventory_update.api.client.desktop;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.ResourceLocation;

/** A Core-owned synchronized menu embedded in an existing built-in window. */
public interface DesktopEmbeddedSession<T extends AbstractContainerMenu> {
    T menu();
    int sessionId();
    boolean isOpen();
    /** Request the normal authenticated server close; disposal remains Core-owned. */
    void close();
    boolean sendPayload(ResourceLocation channel, byte[] data);
}
