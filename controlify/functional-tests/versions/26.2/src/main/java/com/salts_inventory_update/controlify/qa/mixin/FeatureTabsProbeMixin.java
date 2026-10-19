package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.salts_inventory_update.controlify.TuckedWindowTabs", remap = false)
public abstract class FeatureTabsProbeMixin {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private static void observeNames(GuiGraphicsExtractor graphics, Map<UUID, DesktopWorkspaceWindow> windows, DesktopWorkspace desktop, CallbackInfo ci) {
        QaFeatureProbe.beginTabs(windows);
        com.salts_inventory_update.controlify.qa.QaNativeGuideProbe.beginTabs();
    }
    @Inject(method = "frame", at = @At("HEAD"), remap = false)
    private static void observeFrame(GuiGraphicsExtractor graphics, int width, int height, CallbackInfo ci) { QaFeatureProbe.tab(); com.salts_inventory_update.controlify.qa.QaNativeGuideProbe.tab(graphics, width, height); }
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private static void finishNames(GuiGraphicsExtractor graphics, Map<UUID, DesktopWorkspaceWindow> windows, DesktopWorkspace desktop, CallbackInfo ci) {
        QaFeatureProbe.finishTabs();
    }
}
