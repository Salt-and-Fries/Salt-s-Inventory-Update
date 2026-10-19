package com.salts_inventory_update.controlify.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.salts_inventory_update.controlify.model.WorkspaceTooltipCoordinates;
import com.salts_inventory_update.controlify.render.WorkspaceRenderTransform;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native1.21.1 draws tooltips immediately; preserve screen positioning independently of pane scale. */
@Mixin(GuiGraphics.class)
public abstract class WorkspaceTooltipMixin {
    @Shadow public abstract PoseStack pose();
    @Shadow public abstract void flush();
    @Unique private boolean saltsControlify$tooltipPose;
    @ModifyVariable(method="renderTooltipInternal",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private int saltsControlify$tooltipX(int x){return WorkspaceTooltipCoordinates.screenX(x);}
    @ModifyVariable(method="renderTooltipInternal",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private int saltsControlify$tooltipY(int y){return WorkspaceTooltipCoordinates.screenY(y);}
    @Inject(method="renderTooltipInternal",at=@At("HEAD"))
    private void saltsControlify$screenPose(CallbackInfo ci){
        saltsControlify$tooltipPose=WorkspaceRenderTransform.active();
        if(saltsControlify$tooltipPose){flush();pose().pushPose();pose().last().pose().identity();pose().last().normal().identity();}
    }
    @Inject(method="renderTooltipInternal",at=@At("RETURN"))
    private void saltsControlify$restorePose(CallbackInfo ci){if(saltsControlify$tooltipPose){flush();pose().popPose();saltsControlify$tooltipPose=false;}}
}
