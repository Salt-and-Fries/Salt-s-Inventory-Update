package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int salts_inventory_update$leftPos();

    @Accessor("topPos")
    int salts_inventory_update$topPos();

    @Accessor("imageWidth")
    int salts_inventory_update$imageWidth();

    @Accessor("imageHeight")
    int salts_inventory_update$imageHeight();

    @Accessor("hoveredSlot")
    void salts_inventory_update$setHoveredSlot(Slot slot);

    @Accessor("quickCraftSlots")
    Set<Slot> salts_inventory_update$quickCraftSlots();

    @Accessor("isQuickCrafting")
    boolean salts_inventory_update$isQuickCrafting();

    @Accessor("isQuickCrafting")
    void salts_inventory_update$setQuickCrafting(boolean quickCrafting);

    @Accessor("quickCraftingType")
    int salts_inventory_update$quickCraftingType();

    @Accessor("quickCraftingType")
    void salts_inventory_update$setQuickCraftingType(int quickCraftingType);

    @Accessor("quickCraftingRemainder")
    int salts_inventory_update$quickCraftingRemainder();

    @Accessor("quickCraftingRemainder")
    void salts_inventory_update$setQuickCraftingRemainder(int quickCraftingRemainder);

    @Accessor("skipNextRelease")
    void salts_inventory_update$setSkipNextRelease(boolean skipNextRelease);

    @Invoker("renderTooltip")
    void salts_inventory_update$renderTooltip(GuiGraphics graphics, int mouseX, int mouseY);

    @Invoker("renderSlot")
    void salts_inventory_update$renderSlot(GuiGraphics graphics, Slot slot);

    @Invoker("findSlot")
    Slot salts_inventory_update$findSlot(double mouseX, double mouseY);
}
