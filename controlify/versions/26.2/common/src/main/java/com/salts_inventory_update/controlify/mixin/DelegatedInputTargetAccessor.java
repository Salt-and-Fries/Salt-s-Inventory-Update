package com.salts_inventory_update.controlify.mixin;

import dev.isxander.controlify.screenop.keyboard.InputTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Unwrap the native keyboard's Enter/Escape delegate without reflection or replacing it. */
@Mixin(value = InputTarget.Delegated.class, remap = false)
public interface DelegatedInputTargetAccessor {
    @Accessor("target") InputTarget saltsControlify$target();
}
