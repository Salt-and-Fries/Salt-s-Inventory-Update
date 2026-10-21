package com.salts_inventory_update.internal.desktop;

import net.minecraft.world.entity.player.Player;

/** Keeps physical container viewers independent of retained menu lifetimes. */
public interface DesktopContainerOpener {
    void salts_inventory_update$setOpenerVisible(Player player, boolean visible);
}
