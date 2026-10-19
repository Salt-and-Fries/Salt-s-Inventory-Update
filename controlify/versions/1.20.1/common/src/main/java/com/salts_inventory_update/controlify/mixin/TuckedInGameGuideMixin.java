package com.salts_inventory_update.controlify.mixin;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import dev.isxander.controlify.gui.guide.InGameButtonGuide;
import dev.isxander.controlify.controller.ControllerEntity;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native2.x guide keeps its own visibility/scale and shifts below the tucked tabs. */
@Mixin(value=InGameButtonGuide.class,remap=false)
public abstract class TuckedInGameGuideMixin {
 @Shadow @Final private ControllerEntity controller;
 @Unique private boolean saltsControlify$pushed;
 @Inject(method="renderHud",at=@At("HEAD"),remap=false)
 private void saltsControlify$begin(GuiGraphics graphics,float partialTick,CallbackInfo ci){
  int inset=controller.genericConfig().config().ingameGuideBottom?0:SaltsControlifyClient.tuckedGuideInset(graphics.guiWidth(),graphics.guiHeight());
  if(inset>0){graphics.pose().pushPose();graphics.pose().translate(0,inset,0);saltsControlify$pushed=true;}
 }
 @Inject(method="renderHud",at=@At("RETURN"),remap=false)
 private void saltsControlify$end(GuiGraphics graphics,float partialTick,CallbackInfo ci){if(saltsControlify$pushed){saltsControlify$pushed=false;graphics.pose().popPose();}}
}
