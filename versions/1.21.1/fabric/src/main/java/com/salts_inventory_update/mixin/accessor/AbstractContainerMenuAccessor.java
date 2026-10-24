package com.salts_inventory_update.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("dataSlots")
    java.util.List<net.minecraft.world.inventory.DataSlot> salts_inventory_update$getDataSlots();

    @org.spongepowered.asm.mixin.gen.Accessor("lastSlots")
    net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> salts_inventory_update$getLastSlots();
    @org.spongepowered.asm.mixin.gen.Accessor("remoteSlots")
    net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> salts_inventory_update$getRemoteSlots();

    @Invoker("addSlot")
    Slot salts_inventory_update$invokeAddSlot(Slot slot);
}
