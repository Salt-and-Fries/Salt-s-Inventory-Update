package com.salts_inventory_update.compat.seasonsextras.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Use the native menu-button channel instead of an active-vanilla-menu-only packet. */
@Pseudo
@Mixin(targets="io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler",remap=false)
abstract class AirConditioningMenuMixin extends AbstractContainerMenu {
    protected AirConditioningMenuMixin(MenuType<?> type,int id){super(type,id);}
    @Shadow(remap=false) public abstract void cycleButton(int button);
    @Override public boolean clickMenuButton(Player player,int button){
        if(button<0 || button>3 || !stillValid(player))return false;
        if(!player.level().isClientSide)cycleButton(button);
        return true;
    }
}
