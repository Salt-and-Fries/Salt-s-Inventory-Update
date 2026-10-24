package com.salts_inventory_update.spatial.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialHudAccess;
import com.salts_inventory_update.spatial.SpatialDesktopAccess;
import com.salts_inventory_update.spatial.SpatialDesktopCanvas;
import com.salts_inventory_update.api.client.desktop.DesktopHudLayout;
import net.minecraft.world.item.ItemStack;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = InventoryDesktopScreen.class, remap = false)
public abstract class SpatialDesktopMixin implements SpatialDesktopAccess {
    @Invoker("isPointOverInteractiveUi") public abstract boolean salts$isPointOverInteractiveUi(double x, double y);
    @Inject(method="desktopWidth",at=@At("RETURN"),cancellable=true)
    private void salts$logicalWidth(CallbackInfoReturnable<Integer> cir) {
        if(SpatialGuiIntegration.active())cir.setReturnValue((int)Math.ceil(SpatialDesktopCanvas.get().width()));
    }
    @Inject(method="desktopHeight",at=@At("RETURN"),cancellable=true)
    private void salts$logicalHeight(CallbackInfoReturnable<Integer> cir) {
        if(SpatialGuiIntegration.active())cir.setReturnValue((int)Math.ceil(SpatialDesktopCanvas.get().height()));
    }
    @Inject(method="detachedHudLayout",at=@At("RETURN"),cancellable=true)
    private void salts$dockInteraction(CallbackInfoReturnable<DesktopHudLayout> cir) {
        cir.setReturnValue(SpatialDesktopCanvas.dock(cir.getReturnValue()));
    }
    @WrapMethod(method="renderCarried")
    private static void salts$physicalCursorStack(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y,Minecraft mc,String count,Operation<Void> original) {
        if(SpatialGuiIntegration.active()&&SpatialGUIRenderer.isExtractingScreen)return;
        original.call(graphics,stack,x,y,mc,count);
    }
    @WrapMethod(method = "renderDesktopHotbarAffordances")
    private void salts$projectDetachedHotbar(GuiGraphicsExtractor graphics, int x, int y, Operation<Void> original) {
        if (SpatialGuiIntegration.active()) {
            if (SpatialGuiIntegration.movement() || org.tastytrash.spatialGUI.SpatialGUI.config.hideHud
                    || org.tastytrash.spatialGUI.SpatialGUI.config.hideHotbar) return;
            var mc = Minecraft.getInstance();
            var canvas=SpatialDesktopCanvas.get();var pose=graphics.pose();pose.pushMatrix();
            try {
                pose.translate((float)canvas.dockX(),(float)canvas.dockY());
                ((SpatialHudAccess)mc.gui.hud).salts$extractHotbarAndDecorations(graphics, mc.getDeltaTracker());
            } finally { pose.popMatrix(); }
        }
        original.call(graphics, x, y);
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void salts$hookDesktop(CallbackInfo ci) { SpatialGuiIntegration.refresh(); }
    @WrapMethod(method = "mouseClicked")
    private boolean salts$outsidePress(MouseButtonEvent event, boolean twice, Operation<Boolean> original) {
        return original.call(SpatialGuiIntegration.outsidePanel() ? new MouseButtonEvent(-10000, -10000, event.buttonInfo()) : event, twice);
    }
    @WrapMethod(method = "mouseReleased")
    private boolean salts$outsideRelease(MouseButtonEvent event, Operation<Boolean> original) {
        return original.call(SpatialGuiIntegration.outsidePanel() ? new MouseButtonEvent(-10000, -10000, event.buttonInfo()) : event);
    }
    // Retain clamped coordinates for captured geometry drags, but never hit an edge slot/control outside the panel.
    @Inject(method = {"windowAt", "slotAt", "recipeBookWindowAt", "detachedHudSlotAt", "popupControlAt(DD)Lcom/salts_inventory_update/client/InventoryDesktopScreen$ControlHit;"}, at = @At("HEAD"), cancellable = true)
    private void salts$noFalseEdgeTarget(double x, double y, CallbackInfoReturnable<Object> cir) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        var canvas=SpatialDesktopCanvas.get();
        if ((x <= 0 || y <= 0 || x >= canvas.width() || y >= canvas.height())
                && SpatialGuiIntegration.outsidePanel()) cir.setReturnValue(null);
    }
}
