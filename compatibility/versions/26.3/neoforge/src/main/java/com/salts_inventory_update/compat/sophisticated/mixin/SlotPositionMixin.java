package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedSlotPositionAccess;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public abstract class SlotPositionMixin implements SophisticatedSlotPositionAccess {
    @Shadow @Final @Mutable public int x;
    @Shadow @Final @Mutable public int y;

    @Override
    public void salts_inventory_update$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
