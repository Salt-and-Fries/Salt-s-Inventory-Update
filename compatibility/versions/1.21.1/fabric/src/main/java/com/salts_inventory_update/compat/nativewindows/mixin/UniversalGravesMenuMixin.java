package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.graves.UniversalGravesWindows;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="eu.pb4.sgui.virtual.inventory.VirtualScreenHandler",remap=false)
public abstract class UniversalGravesMenuMixin extends AbstractContainerMenu {
    protected UniversalGravesMenuMixin(MenuType<?> type,int id){super(type,id);}
    @Override public MenuType<?> getType(){var own=UniversalGravesWindows.typeForMenu(this);return own==null?super.getType():own;}
    @Inject(method="method_7597",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$graveValidity(Player player,CallbackInfoReturnable<Boolean> ci){if(!UniversalGravesWindows.valid(this))ci.setReturnValue(false);}
}
