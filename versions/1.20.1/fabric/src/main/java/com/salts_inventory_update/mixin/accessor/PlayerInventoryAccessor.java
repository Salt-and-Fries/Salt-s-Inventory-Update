package com.salts_inventory_update.mixin.accessor;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The physical inventory, independent of cosmetic render-time getters. */
@Mixin(Player.class)
public interface PlayerInventoryAccessor {
    @Accessor("inventory")
    Inventory salts_inventory_update$getPhysicalInventory();
}
