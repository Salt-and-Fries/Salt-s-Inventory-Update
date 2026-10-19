package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the actual category row and distinguishes native active tabs from stick hover frames. */
@Mixin(targets = "com.salts_inventory_update.controlify.ContextualRadialOverlay", remap = false)
public abstract class FeatureContextualRenderProbeMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="input(FFFFZZZZZZZ)V",at=@At(value="INVOKE",
        target="Lcom/salts_inventory_update/api/client/desktop/DesktopWorkspace;recipeHoverRejected(Ljava/util/UUID;)Z"),remap=false)
    private boolean hoverRejected(com.salts_inventory_update.api.client.desktop.DesktopWorkspace desktop,java.util.UUID source,
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        boolean rejected=original.call(desktop,source);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.recipeRejected(rejected);return rejected;
    }
    @Inject(method = "renderRecipes", at = @At("HEAD"), remap = false)
    private void alphabetBegin(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetBegin();
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.recipesBefore();
    }
    @Inject(method = "renderRecipes", at = @At("RETURN"), remap = false)
    private void alphabetEnd(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetEnd();
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.recipesAfter();
    }
    @Inject(method="renderNativeFrame",at=@At("HEAD"),remap=false)
    private void frameBefore(GuiGraphicsExtractor graphics,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect virtual,boolean wheel,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeFrameBefore(virtual,wheel);
    }
    @Inject(method="renderNativeFrame",at=@At("RETURN"),remap=false)
    private void frameAfter(GuiGraphicsExtractor graphics,com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect virtual,boolean wheel,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeFrameAfter();
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="renderNativeFrame",at=@At(value="INVOKE",
        target="Lcom/salts_inventory_update/api/client/desktop/DesktopWorkspace;renderWindowFrame(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lcom/salts_inventory_update/api/client/desktop/DesktopWindowBounds;Lnet/minecraft/network/chat/Component;)V"),remap=false)
    private void frameApi(com.salts_inventory_update.api.client.desktop.DesktopWorkspace desktop,GuiGraphicsExtractor graphics,
        com.salts_inventory_update.api.client.desktop.DesktopWindowBounds bounds,net.minecraft.network.chat.Component title,
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.nativeFrameApi(graphics,bounds,title);
        original.call(desktop,graphics,bounds,title);
    }
    @Inject(method = "renderAlphabetMarker", at = @At("HEAD"), remap = false)
    private static void alphabetBefore(GuiGraphicsExtractor graphics, com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Group group,
                                       int count, boolean highlighted,com.salts_inventory_update.controlify.model.AlphabeticalMarkerLayout.Placement placement, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetBefore(group,placement);
    }
    @Inject(method = "renderAlphabetMarker", at = @At("RETURN"), remap = false)
    private static void alphabetAfter(GuiGraphicsExtractor graphics, com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Group group,
                                      int count, boolean highlighted,com.salts_inventory_update.controlify.model.AlphabeticalMarkerLayout.Placement placement, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetAfter();
    }
    @Inject(method = "renderAlphabetAnchor",at=@At("HEAD"),remap=false)
    private static void anchorBefore(GuiGraphicsExtractor graphics,com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Group group,
        int count,boolean highlighted,int x,int y,CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetAnchorBefore(group,count,x,y);
    }
    @Inject(method = "renderAlphabetAnchor",at=@At("RETURN"),remap=false)
    private static void anchorAfter(GuiGraphicsExtractor graphics,com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Group group,
        int count,boolean highlighted,int x,int y,CallbackInfo callback) { com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.alphabetAnchorAfter(); }
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void overlayBefore(GuiGraphicsExtractor graphics, int width, int height, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaNativeGuideProbe.overlayBegin();
        com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe.overlayViewport(width,height);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.artworkBefore(
            (com.salts_inventory_update.controlify.ContextualRadialOverlay)(Object)this,graphics,width,height);
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.runGuardControl(graphics);
    }
    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void overlayAfter(GuiGraphicsExtractor graphics, int width, int height, CallbackInfo callback) {
        com.salts_inventory_update.controlify.qa.QaNativeGuideProbe.overlayEnd();
        com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe.artworkAfter();
    }
    @Inject(method = "renderCategories", at = @At("HEAD"), remap = false)
    private void categoryBefore(GuiGraphicsExtractor graphics, CallbackInfo callback) { QaFeatureProbe.beginCategoryRow(); }
    @Inject(method = "categoryTile", at = @At("HEAD"), remap = false)
    private static void categoryDraw(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean selected, CallbackInfo callback) {
        QaFeatureProbe.categoryTile(selected);
    }
    @Inject(method = "renderCategories", at = @At("TAIL"), remap = false)
    private void categoryAfter(GuiGraphicsExtractor graphics, CallbackInfo callback) { QaFeatureProbe.finishCategoryRow(); }
    @Inject(method = "renderTabs", at = @At("HEAD"), remap = false)
    private void tabsBefore(GuiGraphicsExtractor graphics, CallbackInfo callback) { QaFeatureProbe.beginCreativeTabs(); }
    @Inject(method = "tabTile", at = @At("HEAD"), remap = false)
    private static void tabDraw(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean hovered, boolean active, CallbackInfo callback) {
        QaFeatureProbe.creativeTab(hovered, active);
    }
    @Inject(method = "renderTabs", at = @At("TAIL"), remap = false)
    private void tabsAfter(GuiGraphicsExtractor graphics, CallbackInfo callback) { QaFeatureProbe.finishCreativeTabs(); }
    @Inject(method = "renderSelectedRecipe", at = @At("HEAD"), remap = false)
    private void previewDraw(GuiGraphicsExtractor graphics, com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe recipe, CallbackInfo callback) {
        QaFeatureProbe.recipePreview(recipe);
    }
}
