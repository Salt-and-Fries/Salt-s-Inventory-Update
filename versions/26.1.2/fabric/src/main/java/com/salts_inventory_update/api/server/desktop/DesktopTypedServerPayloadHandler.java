package com.salts_inventory_update.api.server.desktop;

import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface DesktopTypedServerPayloadHandler<T extends AbstractContainerMenu, P> {
    void handle(DesktopServerPayloadContext<T> context, P payload);
}
