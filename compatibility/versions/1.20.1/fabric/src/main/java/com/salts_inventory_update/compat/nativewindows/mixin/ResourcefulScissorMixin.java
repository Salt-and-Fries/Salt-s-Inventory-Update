package com.salts_inventory_update.compat.nativewindows.mixin;

import com.mojang.blaze3d.platform.Window;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ResourcefulLib uses the physical display height even when drawing into another target. */
@Pseudo
@Mixin(targets="com.teamresourceful.resourcefullib.client.utils.RenderUtils",remap=false)
public abstract class ResourcefulScissorMixin {
    @Redirect(method={"getScissorBounds","getScissorRect"},at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getHeight()I",remap=true))
    private static int salts_inventory_update$captureHeight(Window window) {
        return DesktopNativeRenderTarget.current().map(DesktopNativeRenderTarget::pixelHeight).orElseGet(window::getHeight);
    }
    @Redirect(method={"getScissorBounds","getScissorRect"},at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScale()D",remap=true))
    private static double salts_inventory_update$captureScale(Window window) {
        return DesktopNativeRenderTarget.current().map(DesktopNativeRenderTarget::guiScale).orElseGet(window::getGuiScale);
    }
}
