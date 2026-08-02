package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.DesktopMouseHandoff;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMouseHandoffMixin {
    @Shadow
    @Final
    public MouseHandler mouseHandler;

    @Shadow
    public @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void salts_inventory_update$beginDesktopMouseHandoff(@Nullable Screen incomingScreen, CallbackInfo ci) {
        if (incomingScreen == null && this.screen instanceof InventoryDesktopScreen) {
            ((DesktopMouseHandoff) this.mouseHandler).salts_inventory_update$beginDesktopMouseHandoff();
        }
    }
}
