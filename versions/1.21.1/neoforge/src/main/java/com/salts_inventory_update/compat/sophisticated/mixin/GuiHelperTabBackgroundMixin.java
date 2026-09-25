package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;

import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;

/** Lets each hosted Sophisticated tab use Salt's window plate while preserving native tab contents. */
@Mixin(value = GuiHelper.class, remap = false)
public abstract class GuiHelperTabBackgroundMixin {
    @Inject(method = "renderTabBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private static void salts_inventory_update$replaceHostedTabBackground(
        GuiGraphics graphics,
        int x,
        int y,
        int width,
        int height,
        CallbackInfo ci
    ) {
        if (SophisticatedHostedScreenBridge.useSaltTabBackgrounds()) {
            ci.cancel();
        }
    }
}
