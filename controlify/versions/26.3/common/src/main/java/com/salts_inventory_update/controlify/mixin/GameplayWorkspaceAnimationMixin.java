package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains the native HUD and completes the tuck transition during gameplay. */
@Mixin(Hud.class)
public abstract class GameplayWorkspaceAnimationMixin {
    @Inject(method = {"extractItemHotbar", "extractCrosshair"}, at = @At("HEAD"), cancellable = true)
    private void saltsControlify$hideWindowHud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof RadialInventoryScreen) ci.cancel();
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void saltsControlify$extractTuckedTransition(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        SaltsControlifyClient.renderGameplayTransition(graphics, delta.getGameTimeDeltaPartialTick(false));
    }
}