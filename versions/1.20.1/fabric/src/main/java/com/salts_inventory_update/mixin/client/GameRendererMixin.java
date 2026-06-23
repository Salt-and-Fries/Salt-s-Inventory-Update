package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.InventoryDesktopScreen;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void salts_inventory_update$pickDesktopCursorTarget(float tickProgress, CallbackInfo ci) {
        InventoryDesktopScreen.updateDesktopCursorTarget(Minecraft.getInstance());
    }
}
