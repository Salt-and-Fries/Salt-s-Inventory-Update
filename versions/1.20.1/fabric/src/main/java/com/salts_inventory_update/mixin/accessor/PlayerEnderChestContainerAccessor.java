package com.salts_inventory_update.mixin.accessor;

import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEnderChestContainer.class)
public interface PlayerEnderChestContainerAccessor {
    @Accessor("activeChest")
    EnderChestBlockEntity salts_inventory_update$activeChest();
}
