package com.salts_inventory_update.compat.ae2.menu;

import appeng.menu.AEBaseMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Desktop opening additions implemented on the upstream native builder. */
public interface DesktopMenuBuilder {
    void salts$writeOpeningData(AEBaseMenu menu, RegistryFriendlyByteBuf output);
    AEBaseMenu salts$createDesktopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf input);
}
