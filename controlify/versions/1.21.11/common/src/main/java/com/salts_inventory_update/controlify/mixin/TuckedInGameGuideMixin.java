package com.salts_inventory_update.controlify.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import dev.isxander.controlify.gui.guide.InGameButtonGuide;
import dev.isxander.controlify.gui.guide.GuideDomain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Preserve Controlify3.0's gameplay guide and move its top edge below retained tabs. */
@Mixin(value=InGameButtonGuide.class,remap=false)
public abstract class TuckedInGameGuideMixin {
    @WrapOperation(method="renderHud",at=@At(value="INVOKE",target="Ldev/isxander/controlify/gui/guide/GuideRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Ldev/isxander/controlify/gui/guide/GuideDomain;Lnet/minecraft/client/Minecraft;ZZ)V"),remap=false)
    private void saltsControlify$clearTabs(GuiGraphics graphics,GuideDomain<?> guide,Minecraft minecraft,boolean bottom,boolean contrast,Operation<Void> original) {
        int inset=bottom?0:SaltsControlifyClient.tuckedGuideInset(graphics.guiWidth(),graphics.guiHeight());
        if(inset<=0){original.call(graphics,guide,minecraft,bottom,contrast);return;}
        graphics.pose().pushMatrix();
        try{graphics.pose().translate(0,inset);original.call(graphics,guide,minecraft,bottom,contrast);}finally{graphics.pose().popMatrix();}
    }
}
