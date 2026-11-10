package com.salts_inventory_update.compat.zenith.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.compat.zenith.ZenithWindows;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Bind native stat/clue generation to its actual menu during cross-window transfers. */
@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu",remap=false)
public abstract class ZenithEnchantingContextMixin {
    // The publisher is distributed in Fabric's intermediary namespace; both
    // names support production and the named development environment.
    @WrapMethod(method={"slotsChanged","method_7609"},remap=false)
    private void salts$offerOwner(Container inventory,Operation<Void> original){
        ZenithWindows.generating((AbstractContainerMenu)(Object)this,()->original.call(inventory));
    }
    @WrapMethod(method="gatherStats()V",remap=false)
    private void salts$statsOwner(Operation<Void> original){
        ZenithWindows.generating((AbstractContainerMenu)(Object)this,()->original.call());
    }
}
