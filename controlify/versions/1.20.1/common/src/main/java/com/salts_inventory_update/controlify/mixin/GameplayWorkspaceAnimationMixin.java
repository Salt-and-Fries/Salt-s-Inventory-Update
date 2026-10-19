package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes the upward tuck animation after gameplay already regained its screen and controls. */
@Mixin(Gui.class)
public abstract class GameplayWorkspaceAnimationMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void saltsControlify$hideWindowHotbar(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof RadialInventoryScreen) ci.cancel();
    }
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void saltsControlify$hideWindowCrosshair(GuiGraphics graphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof RadialInventoryScreen) ci.cancel();
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void saltsControlify$renderTuckedTransition(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        SaltsControlifyClient.renderGameplayTransition(com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor.wrap(graphics), partialTick);
    }
}
