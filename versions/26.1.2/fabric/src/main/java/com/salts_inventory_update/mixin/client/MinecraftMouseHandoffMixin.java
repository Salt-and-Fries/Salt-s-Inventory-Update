package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.DesktopMouseHandoff;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMouseHandoffMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void salts_inventory_update$beginDesktopMouseHandoff(@Nullable Screen incomingScreen, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (incomingScreen == null && minecraft.screen instanceof InventoryDesktopScreen) {
            ((DesktopMouseHandoff) minecraft.mouseHandler).salts_inventory_update$beginDesktopMouseHandoff();
        }
    }
}
