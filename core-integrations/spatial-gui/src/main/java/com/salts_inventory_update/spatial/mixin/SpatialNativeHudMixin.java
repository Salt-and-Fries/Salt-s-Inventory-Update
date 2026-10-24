package com.salts_inventory_update.spatial.mixin;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialHudAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(value = Hud.class, priority = 1500)
public abstract class SpatialNativeHudMixin implements SpatialHudAccess {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void salts$unclippedCarriedFeedback(GuiGraphicsExtractor graphics,DeltaTracker tracker,CallbackInfo ci) {
        if(!SpatialGuiIntegration.active()||SpatialGuiIntegration.movement()||SpatialGUIRenderer.isExtractingScreen)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        double scale=mc.getWindow().getGuiScale();
        int x=(int)Math.clamp(org.tastytrash.spatialGUI.util.MouseHandlerUtil.getSourceX()/scale,9,mc.getWindow().getGuiScaledWidth()-9);
        int y=(int)Math.clamp(org.tastytrash.spatialGUI.util.MouseHandlerUtil.getSourceY()/scale,9,mc.getWindow().getGuiScaledHeight()-9);
        SpatialGuiIntegration.desktop().workspaceAccess().renderCarried(graphics,x,y);
    }
    @Invoker("extractHotbarAndDecorations") public abstract void salts$extractHotbarAndDecorations(GuiGraphicsExtractor graphics, DeltaTracker tracker);
    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void salts$hotbarOnItsInteractionPlane(GuiGraphicsExtractor graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (SpatialGuiIntegration.active() && !SpatialGuiIntegration.movement() && !SpatialGUIRenderer.isExtractingScreen) ci.cancel();
    }
}
