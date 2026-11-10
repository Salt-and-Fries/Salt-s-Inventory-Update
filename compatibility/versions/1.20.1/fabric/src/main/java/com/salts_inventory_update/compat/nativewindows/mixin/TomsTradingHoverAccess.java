package com.salts_inventory_update.compat.nativewindows.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class) public interface TomsTradingHoverAccess {
    @Accessor("hoveredSlot") Slot salts_inventory_compat$hoveredSlot();
    @Accessor("hoveredSlot") void salts_inventory_compat$hoveredSlot(Slot slot);
}
