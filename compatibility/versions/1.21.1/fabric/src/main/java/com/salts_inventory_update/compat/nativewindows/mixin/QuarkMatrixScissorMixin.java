package com.salts_inventory_update.compat.nativewindows.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** The native docket uses physical-display scissors. Translate them through the GUI capture. */
@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.client.screen.MatrixEnchantingPieceList",remap=false)
public abstract class QuarkMatrixScissorMixin {
    @Redirect(method="renderWidget",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V",remap=false))
    private void clip(int x,int y,int width,int height,GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(DesktopNativeRenderTarget.current().isEmpty()) { RenderSystem.enableScissor(x,y,width,height);return; }
        var window=Minecraft.getInstance().getWindow();double scale=window.getGuiScale();int bottom=window.getGuiScaledHeight();
        graphics.enableScissor((int)(x/scale),bottom-(int)((y+height)/scale),(int)((x+width)/scale),bottom-(int)(y/scale));
    }
    @Redirect(method="renderWidget",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderSystem;disableScissor()V",remap=false))
    private void unclip(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        if(DesktopNativeRenderTarget.current().isPresent())graphics.disableScissor();else RenderSystem.disableScissor();
    }
}

