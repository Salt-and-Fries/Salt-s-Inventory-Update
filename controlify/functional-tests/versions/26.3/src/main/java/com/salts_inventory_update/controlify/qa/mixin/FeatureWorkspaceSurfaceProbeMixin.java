package com.salts_inventory_update.controlify.qa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.salts_inventory_update.controlify.RadialInventoryScreen", remap = false)
public abstract class FeatureWorkspaceSurfaceProbeMixin {
    @Inject(method="renderRecipeHoverGhosts",at=@At("HEAD"),remap=false)
    private void ghostsBefore(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.ghostsBefore(id,rect);
    }
    @Inject(method="renderRecipeHoverGhosts",at=@At("RETURN"),remap=false)
    private void ghostsAfter(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.ghostsAfter();
    }
    @Inject(method="extractRenderState",at=@At("HEAD"),remap=false)
    private void backdropBefore(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.workspaceBefore();
    }
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"),remap=false)
    private void backdrop(GuiGraphicsExtractor graphics,int x1,int y1,int x2,int y2,int color,Operation<Void> original) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.backdrop(x1,y1,x2,y2,color);
        original.call(graphics,x1,y1,x2,y2,color);
    }
    @Inject(method="extractRenderState",at=@At("RETURN"),remap=false)
    private void backdropAfter(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.workspaceAfter(contextual);
        com.salts_inventory_update.controlify.qa.QaStatusEffectLayouts.render(graphics);
    }
    @Inject(method="renderLiveDestination",at=@At("HEAD"),remap=false)
    private void destinationBefore(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.destinationBefore(id,rect);
    }
    @Inject(method="renderLiveDestination",at=@At("RETURN"),remap=false)
    private void destinationAfter(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.destinationAfter();
    }
    @org.spongepowered.asm.mixin.Shadow private UUID hoveredWindow;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private com.salts_inventory_update.controlify.ContextualRadialOverlay contextual;
    @Inject(method="controllerContextualInput",at=@At("RETURN"),remap=false)
    private void contextualRelease(boolean held,float rx,float ry,float lx,float ly,boolean cl,boolean cr,boolean filter,
        boolean confirm,boolean leftHeld,boolean rightHeld,boolean craftStack,CallbackInfoReturnable<Boolean> callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.releaseResult(held,callback.getReturnValue(),contextual);
    }
    @Inject(method="controllerClickSlot",at=@At("HEAD"),remap=false)
    private void nativeSelect(net.minecraft.world.inventory.Slot slot,int slotId,int button,net.minecraft.world.inventory.ContainerInput type,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.click(slot,hoveredWindow,contextual);
    }
    @Inject(method = "renderLiveSource",at=@At("HEAD"),remap=false)
    private void liveBefore(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.sourceBefore(id,rect);
    }
    @Inject(method = "renderLiveSource",at=@At("RETURN"),remap=false)
    private void liveAfter(GuiGraphicsExtractor graphics,UUID id,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect rect,float tick,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.sourceAfter();
    }
    @Inject(method = "renderWorkspaceContents", at = @At("HEAD"), remap = false)
    private void begin(GuiGraphicsExtractor graphics, float tick, CallbackInfo callback) { QaVisualFeatureProbe.workspaceBegin(); }
    @Inject(method = "renderWorkspaceContents", at = @At("RETURN"), remap = false)
    private void end(GuiGraphicsExtractor graphics, float tick, CallbackInfo callback) { QaVisualFeatureProbe.workspaceEnd(); }
    @WrapOperation(method = "renderWorkspaceContents", at = @At(value = "INVOKE",
        target = "Lcom/salts_inventory_update/controlify/model/WorkspaceSurfaceTransition;value(Ljava/lang/Object;ZJZ)D"), remap = false)
    private double surface(com.salts_inventory_update.controlify.model.WorkspaceSurfaceTransition<UUID> transition,
                           Object id, boolean live, long now, boolean animate, Operation<Double> original) {
        double value = original.call(transition, id, live, now, animate);
        QaVisualFeatureProbe.surface((UUID) id, live, value);
        return value;
    }
    @WrapOperation(method = "renderNativeWindow", at = @At(value = "INVOKE",
        target = "Lcom/salts_inventory_update/api/client/desktop/DesktopWorkspace;renderWindow(Ljava/util/UUID;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"), remap = false)
    private void nativePane(DesktopWorkspace desktop, UUID id, GuiGraphicsExtractor graphics, int x, int y, float tick, Operation<Void> original) {
        QaVisualFeatureProbe.nativePane(id);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeSource(graphics,id);
        original.call(desktop,id,graphics,x,y,tick);
    }
}
