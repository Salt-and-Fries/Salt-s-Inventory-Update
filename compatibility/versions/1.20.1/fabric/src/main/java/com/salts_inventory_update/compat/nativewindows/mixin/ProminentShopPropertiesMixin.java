package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.prominent.ProminentShopWindows;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Run after Owo merges its property method; absent Owo is an intentional no-op. */
@Mixin(value=AbstractContainerMenu.class,priority=900)
public abstract class ProminentShopPropertiesMixin {
    @Inject(method="syncProperties",at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void salts_inventory_compat$addressedShopProperties(CallbackInfo ci){
        if(ProminentShopWindows.ownsProperties((AbstractContainerMenu)(Object)this))ci.cancel();
    }
}
