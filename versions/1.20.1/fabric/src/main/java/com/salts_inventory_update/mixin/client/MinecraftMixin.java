package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.debug.DesktopDebug;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private static int salts_inventory_update$setScreenProbeLogs;
    @Unique
    private static int salts_inventory_update$handleKeybindProbeLogs;

    @Shadow
    public Options options;

    @Shadow
    public @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$guardSingletonDesktopScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (salts_inventory_update$setScreenProbeLogs < 32) {
            salts_inventory_update$setScreenProbeLogs++;
            DesktopDebug.probe(
                "mixin Minecraft.setScreen incoming={} current={} runtime={}",
                screen == null ? "null" : screen.getClass().getName(),
                this.screen == null ? "null" : this.screen.getClass().getName(),
                SaltsInventoryRuntime.isEnabled()
            );
        }
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

        int consumed = 0;
        while (this.options.keyInventory.consumeClick()) {
            consumed++;
            DesktopDebug.trace("client consumed legacy inventory key click; release/hold controller owns E");
        }
        if (consumed > 0 || salts_inventory_update$handleKeybindProbeLogs < 8) {
            salts_inventory_update$handleKeybindProbeLogs++;
            DesktopDebug.probe(
                "mixin Minecraft.handleKeybinds runtime={} screen={} consumedInventoryClicks={}",
                SaltsInventoryRuntime.isEnabled(),
                this.screen == null ? "null" : this.screen.getClass().getName(),
                consumed
            );
        }
    }

}
