package com.salts_inventory_update.compat.nativewindows.mixin;

import com.mojang.blaze3d.platform.Window;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Owo's pixel scissors must use the active native capture rather than the display. */
@Pseudo
@Mixin(targets="io.wispforest.owo.ui.util.ScissorStack",remap=false)
public abstract class OwoScissorMixin {
    @Redirect(method="applyState",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getHeight()I",remap=true))
    private static int salts_inventory_compat$height(Window window){return DesktopNativeRenderTarget.current().map(DesktopNativeRenderTarget::pixelHeight).orElseGet(window::getHeight);}
    @Redirect(method="applyState",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getWidth()I",remap=true))
    private static int salts_inventory_compat$width(Window window){return DesktopNativeRenderTarget.current().map(DesktopNativeRenderTarget::pixelWidth).orElseGet(window::getWidth);}
    @Redirect(method="applyState",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScale()D",remap=true))
    private static double salts_inventory_compat$scale(Window window){return scale(window);}
    @Redirect(method="pushDirect",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScale()D",remap=true))
    private static double salts_inventory_compat$directScale(Window window){return scale(window);}
    @Redirect(method="pushDirect",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I",remap=true))
    private static int salts_inventory_compat$scaledHeight(Window window){return DesktopNativeRenderTarget.current().map(t->(int)Math.ceil(t.pixelHeight()/t.guiScale())).orElseGet(window::getGuiScaledHeight);}
    private static double scale(Window window){return DesktopNativeRenderTarget.current().map(DesktopNativeRenderTarget::guiScale).orElseGet(window::getGuiScale);}
}
