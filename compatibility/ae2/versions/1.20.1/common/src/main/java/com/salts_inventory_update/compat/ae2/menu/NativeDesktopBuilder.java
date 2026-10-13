package com.salts_inventory_update.compat.ae2.menu;

import appeng.menu.AEBaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Desktop-only bridge implemented on AE2's existing builder by its integration patch. */
public interface NativeDesktopBuilder {
    void writeDesktopOpeningData(AEBaseMenu menu, FriendlyByteBuf output);
    AEBaseMenu createDesktopMenu(int id, Inventory inventory, FriendlyByteBuf openingData);
}
