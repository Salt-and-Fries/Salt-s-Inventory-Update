package com.salts_inventory_update.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.WindowedInventoryClient;

// Run visibility checks before HUD decorators start their scoped transforms.
@Mixin(value = Gui.class, priority = 1100)
public abstract class GuiMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hideHotbarForDesktop(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (WindowedInventoryClient.shouldHideHotbar()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hideCrosshairForDesktop(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (WindowedInventoryClient.shouldHideCrosshair()) {
            ci.cancel();
        }
    }

}
