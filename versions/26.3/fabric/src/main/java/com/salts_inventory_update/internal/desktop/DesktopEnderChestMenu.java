package com.salts_inventory_update.internal.desktop;

import net.minecraft.world.level.block.entity.EnderChestBlockEntity;

/** Physical opener identity; item storage remains the player's shared ender inventory. */
public interface DesktopEnderChestMenu {
    boolean salts_inventory_update$ownsEnderChest(EnderChestBlockEntity chest);
}
