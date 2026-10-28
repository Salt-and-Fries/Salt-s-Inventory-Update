package com.salts_inventory_update.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AutomaticContainerScreenAccessor {
    @Accessor("leftPos") int salts_inventory_update$nativeLeft();
    @Accessor("topPos") int salts_inventory_update$nativeTop();
    @Accessor("imageWidth") int salts_inventory_update$nativeWidth();
    @Accessor("imageHeight") int salts_inventory_update$nativeHeight();
    @Accessor("inventoryLabelY") int salts_inventory_update$nativeInventoryLabelY();
    @Accessor("hoveredSlot") void salts_inventory_update$nativeHovered(Slot slot);
    @Invoker("renderSlot") void salts_inventory_update$drawNativeSlot(net.minecraft.client.gui.GuiGraphics graphics, Slot slot,int mouseX,int mouseY);
}
