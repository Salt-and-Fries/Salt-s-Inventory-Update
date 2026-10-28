package com.salts_inventory_update.mixin.client;
import java.util.List;
import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphics.class)
public abstract class AutomaticGuiGraphicsMixin {
    @Inject(method="innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",at=@At("HEAD"))
    private void salts_inventory_update$observe(com.mojang.blaze3d.pipeline.RenderPipeline pipeline,Identifier texture,int x1,int x2,int y1,int y2,float u0,float u1,float v0,float v1,int color,CallbackInfo ci) {
        AutomaticNativeScope.recordDraw((GuiGraphics)(Object)this,x1,x2,y1,y2);
        AutomaticNativeScope.beginTextureClip((GuiGraphics)(Object)this,x1,x2,y1,y2);
    }
    @Inject(method="innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",at=@At("TAIL"))
    private void salts_inventory_update$restoreClip(CallbackInfo ci){AutomaticNativeScope.finishTextureClip(AutomaticNativeScope.current(),(GuiGraphics)(Object)this);}
    @Inject(method="setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Z)V",at=@At("HEAD"),cancellable=true)
    private void salts_inventory_update$tooltip(Font font,List<ClientTooltipComponent> lines,int x,int y,ClientTooltipPositioner positioner,Identifier texture,boolean flag,CallbackInfo ci) {
        var f=AutomaticNativeScope.current();if(f==null||f.rendering()==null)return;
        f.owner().tooltipFont=font;f.owner().tooltip=List.copyOf(lines);f.owner().tooltipTexture=texture;ci.cancel();
    }

}
