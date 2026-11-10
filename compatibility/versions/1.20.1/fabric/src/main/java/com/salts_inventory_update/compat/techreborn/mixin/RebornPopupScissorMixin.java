package com.salts_inventory_update.compat.techreborn.mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Reborn already transforms this clip into target coordinates; do not translate twice. */
@Pseudo @Mixin(targets="reborncore.client.gui.config.elements.AbstractConfigPopupElement",remap=false)
public abstract class RebornPopupScissorMixin {
    @Redirect(method="drawState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;enableScissor(IIII)V",remap=true),remap=false)
    private void salts$targetClip(GuiGraphics g,int x,int y,int right,int bottom){
        var target=DesktopNativeRenderTarget.current();if(target.isEmpty()){g.enableScissor(x,y,right,bottom);return;}
        g.flush();double scale=target.get().guiScale();RenderSystem.enableScissor((int)(x*scale),target.get().pixelHeight()-(int)(bottom*scale),(int)((right-x)*scale),(int)((bottom-y)*scale));
    }
    @Redirect(method="drawState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V",remap=true),remap=false)
    private void salts$endClip(GuiGraphics g){if(DesktopNativeRenderTarget.current().isEmpty())g.disableScissor();else{g.flush();RenderSystem.disableScissor();}}
}
