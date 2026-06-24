package com.salts_inventory_update.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.WindowedInventoryClient;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hideCrosshairForDesktop(GuiGraphics graphics, CallbackInfo ci) {
        if (WindowedInventoryClient.shouldHideCrosshair()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void salts_inventory_update$extractPassiveGhostWindows(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        WindowedInventoryClient.extractPassiveGhostWindows(GuiGraphicsExtractor.wrap(graphics));
    }
}
