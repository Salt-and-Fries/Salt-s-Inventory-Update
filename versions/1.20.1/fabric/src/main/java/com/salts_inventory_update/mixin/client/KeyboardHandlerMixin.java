package com.salts_inventory_update.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import com.salts_inventory_update.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.InventoryKeyHoldController;
import com.salts_inventory_update.debug.DesktopDebug;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Unique
    private static int salts_inventory_update$keyProbeLogs;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$handleInventoryKeyHold(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        boolean inventoryKey = this.minecraft.options.keyInventory.matches(key, scancode);
        boolean handled = InventoryKeyHoldController.handleInventoryKeyAction(this.minecraft, action, new KeyEvent(key, scancode, modifiers));
        if (inventoryKey && salts_inventory_update$keyProbeLogs < 24) {
            salts_inventory_update$keyProbeLogs++;
            DesktopDebug.probe(
                "mixin KeyboardHandler.keyPress inventoryKey window={} key={} scancode={} action={} modifiers={} handled={} screen={}",
                window,
                key,
                scancode,
                action,
                modifiers,
                handled,
                this.minecraft.screen == null ? "null" : this.minecraft.screen.getClass().getName()
            );
        }
        if (handled) {
            ci.cancel();
        }
    }
}
