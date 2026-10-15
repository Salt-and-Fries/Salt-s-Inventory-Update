package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the actual Core catalog action and its client hotbar result without changing it. */
@Mixin(InventoryDesktopScreen.class)
public abstract class FeatureCreativeProbeMixin {
    @Inject(method = "workspaceCreativeHotbar", at = @At("HEAD"), remap = false)
    private void beforeCreativeAction(ItemStack source, boolean stack, CallbackInfo callback) {
        QaFeatureProbe.creativeAction("before", source, stack);
    }

    @Inject(method = "workspaceCreativeHotbar", at = @At("RETURN"), remap = false)
    private void afterCreativeAction(ItemStack source, boolean stack, CallbackInfo callback) {
        QaFeatureProbe.creativeAction("after", source, stack);
    }
}
