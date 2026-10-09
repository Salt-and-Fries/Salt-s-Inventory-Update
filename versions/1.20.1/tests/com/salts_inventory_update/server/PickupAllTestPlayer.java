package com.salts_inventory_update.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;

/** Allocated without a world; only the inventory-click predicates are needed by these tests. */
final class PickupAllTestPlayer extends ServerPlayer {
    private static final Abilities ABILITIES = new Abilities();
    Inventory inventory;

    private PickupAllTestPlayer() { super(null, null, null); }
    @Override public boolean isAlive() { return true; }
    @Override public boolean isSpectator() { return false; }
    @Override public Component getName() { return Component.literal("pickup-all-test"); }
    @Override public Abilities getAbilities() { return ABILITIES; }
    @Override public Inventory getInventory() { return this.inventory; }
}
