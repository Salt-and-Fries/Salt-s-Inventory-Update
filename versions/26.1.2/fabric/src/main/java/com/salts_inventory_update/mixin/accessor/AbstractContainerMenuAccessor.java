package com.salts_inventory_update.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("dataSlots")
    java.util.List<net.minecraft.world.inventory.DataSlot> salts_inventory_update$getDataSlots();

    @Invoker("addSlot")
    Slot salts_inventory_update$invokeAddSlot(Slot slot);
}
