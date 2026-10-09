package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.InventoryDesktopScreen;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$guardSingletonDesktopScreen(@Nullable Screen incomingScreen, CallbackInfo ci) {
        if (InventoryDesktopScreen.replaceVanillaCreativeScreen(this.minecraft, incomingScreen)) {
            ci.cancel();
            return;
        }

        if (incomingScreen instanceof InventoryDesktopScreen incoming && this.screen == incoming) {
            ci.cancel();
        }
    }
}
