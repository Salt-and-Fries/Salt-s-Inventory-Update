package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
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

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void salts_inventory_update$openWindowedInventory(CallbackInfo ci) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        while (this.options.keyInventory.consumeClick()) {
            DesktopDebug.trace("client consumed legacy inventory key click; release/hold controller owns E");
        }
    }

    @Inject(method = "pick", at = @At("TAIL"))
    private void salts_inventory_update$pickDesktopCursorTarget(float tickProgress, CallbackInfo ci) {
        InventoryDesktopScreen.updateDesktopCursorTarget((Minecraft) (Object) this);
    }
}
