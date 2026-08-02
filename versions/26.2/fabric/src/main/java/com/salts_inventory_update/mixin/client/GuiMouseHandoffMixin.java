package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.DesktopMouseHandoff;
import com.salts_inventory_update.client.InventoryDesktopScreen;
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

@Mixin(Gui.class)
public abstract class GuiMouseHandoffMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void salts_inventory_update$beginDesktopMouseHandoff(@Nullable Screen incomingScreen, CallbackInfo ci) {
        if (incomingScreen == null && this.screen instanceof InventoryDesktopScreen) {
            ((DesktopMouseHandoff) this.minecraft.mouseHandler).salts_inventory_update$beginDesktopMouseHandoff();
        }
    }
}
