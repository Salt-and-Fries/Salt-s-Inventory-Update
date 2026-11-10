package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.minecells.MineCellsWindows;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owning-session synchronization replaces only a hosted crafter's Owo property packet. */
@Mixin(value=AbstractContainerMenu.class,priority=900)
public abstract class MineCellsPropertiesMixin {
    @Inject(method="syncProperties",at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void salts_inventory_compat$addressedCellCrafter(CallbackInfo ci){
        if(MineCellsWindows.ownsProperties((AbstractContainerMenu)(Object)this))ci.cancel();
    }
}
