package com.salts_inventory_update.api.server.desktop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerPayloadContext<T extends AbstractContainerMenu> extends DesktopServerSessionContext<T, Object> {
    ResourceLocation channel();

    byte[] data();
}
