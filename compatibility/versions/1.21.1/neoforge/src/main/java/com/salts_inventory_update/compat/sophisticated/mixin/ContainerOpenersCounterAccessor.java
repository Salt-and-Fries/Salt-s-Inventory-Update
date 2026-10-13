package com.salts_inventory_update.compat.sophisticated.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ContainerOpenersCounter.class)
public interface ContainerOpenersCounterAccessor {
    @Invoker("isOwnContainer")
    boolean salts_inventory_update$isOwnContainer(Player player);
}
