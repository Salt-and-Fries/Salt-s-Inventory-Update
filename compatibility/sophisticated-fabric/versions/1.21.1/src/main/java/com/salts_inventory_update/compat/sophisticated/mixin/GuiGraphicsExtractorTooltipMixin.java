package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;

import net.minecraft.client.gui.GuiGraphics;

/** Moves legacy immediate tooltips into the translated Salt window coordinate space. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsExtractorTooltipMixin {
    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int salts_inventory_update$translateTooltipX(int nativeX) {
        return SophisticatedHostedScreenBridge.translateTooltipX(nativeX);
    }

    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int salts_inventory_update$translateTooltipY(int nativeY) {
        return SophisticatedHostedScreenBridge.translateTooltipY(nativeY);
    }
}
