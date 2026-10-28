package com.salts_inventory_update.compat.alloyforgery.mixin;

import com.salts_inventory_update.compat.alloyforgery.AlloySlotModes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="io.wispforest.alloyforgery.client.AlloyForgeScreen",remap=false)
abstract class AlloyForgeScreenModeMixin extends AbstractContainerScreen<AbstractContainerMenu> implements AlloySlotModes.Source {
    @Shadow private boolean allowSlotToggling;
    protected AlloyForgeScreenModeMixin(AbstractContainerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override public boolean salts$lockingEnabled(){return allowSlotToggling;}
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void salts$observeNativeMode(GuiGraphicsExtractor graphics,int x,int y,float delta,CallbackInfo ci){
        AlloySlotModes.remember(getMenu(),this);
    }
}
