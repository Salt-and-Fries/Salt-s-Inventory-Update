package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.debug.DesktopDebug;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public Options options;

    @Shadow
    public @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$guardSingletonDesktopScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (InventoryDesktopScreen.replaceVanillaCreativeScreen((Minecraft) (Object) this, screen)) {
            ci.cancel();
            return;
        }

        if (screen instanceof InventoryDesktopScreen incoming && this.screen == incoming) {
            ci.cancel();
        }
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void salts_inventory_update$openWindowedInventory(CallbackInfo ci) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        while (this.options.keyInventory.consumeClick()) {
            DesktopDebug.trace("client consumed legacy inventory key click; release/hold controller owns E");
        }
    }

}
