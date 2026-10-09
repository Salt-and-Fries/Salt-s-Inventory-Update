package com.salts_inventory_update.api.server.desktop;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerPayloadContext<T extends AbstractContainerMenu> extends DesktopServerSessionContext<T, Object> {
    Identifier channel();

    byte[] data();
}
