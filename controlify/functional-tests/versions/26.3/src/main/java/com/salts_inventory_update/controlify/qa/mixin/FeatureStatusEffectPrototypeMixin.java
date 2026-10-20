package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.controlify.qa.QaStatusEffectLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only a running disposable experiment replaces the existing side-of-Character effects. */
@Mixin(InventoryDesktopScreen.class)
public abstract class FeatureStatusEffectPrototypeMixin {
    @Inject(method="renderCharacterEffects",at=@At("TAIL"),remap=false)
    private void sideEffects(CallbackInfo callback) {
        if (com.salts_inventory_update.controlify.render.WorkspaceRenderTransform.active())
            com.salts_inventory_update.controlify.qa.QaPlaytestProbe.sideEffectsBodies++;
    }
    @Inject(method = "renderCharacterEffects", at = @At("HEAD"), cancellable = true, remap = false)
    private void prototype(CallbackInfo callback) {
        if (QaStatusEffectLayouts.active()) callback.cancel();
    }
}
