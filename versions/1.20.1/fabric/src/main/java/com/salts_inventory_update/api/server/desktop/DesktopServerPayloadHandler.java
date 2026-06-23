package com.salts_inventory_update.api.server.desktop;

import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface DesktopServerPayloadHandler<T extends AbstractContainerMenu> {
    void handle(DesktopServerPayloadContext<T> context);
}
