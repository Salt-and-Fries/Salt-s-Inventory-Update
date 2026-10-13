package com.salts_inventory_update.compat.ae2.mixin;

import com.salts_inventory_update.compat.ae2.menu.NativeMenuRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Newer AE2 versions expose this additional registration path. */
@Pseudo
@Mixin(targets = "appeng.menu.implementations.MenuTypeBuilder", remap = false)
public abstract class UnregisteredMenuTypeBuilderMixin {
    @Inject(method = "buildUnregistered", at = @At("RETURN"))
    private void salts$captureUnregisteredBuilder(CallbackInfoReturnable<Object> result) {
        NativeMenuRegistry.capture(result.getReturnValue(), this);
    }
}
