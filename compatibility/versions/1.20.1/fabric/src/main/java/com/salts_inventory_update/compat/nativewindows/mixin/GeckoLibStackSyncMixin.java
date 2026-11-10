package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.geckolib.GeckoLibStackSynchronization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The publisher's wrapper still requires the original vanilla stack match. */
@Mixin(value = AbstractContainerMenu.class, priority = 900)
public abstract class GeckoLibStackSyncMixin {
    @Inject(method = "geckolib$xnorGeckolibStackIds", at = @At("HEAD"),
        cancellable = true, remap = false, require = 0)
    private static void salts_inventory_compat$comparePresentIds(
        CompoundTag first, CompoundTag second, CallbackInfoReturnable<Boolean> result
    ) {
        Boolean absent = GeckoLibStackSynchronization.compareMissingIdentities(first, second);
        if (absent != null) result.setReturnValue(absent);
    }
}
