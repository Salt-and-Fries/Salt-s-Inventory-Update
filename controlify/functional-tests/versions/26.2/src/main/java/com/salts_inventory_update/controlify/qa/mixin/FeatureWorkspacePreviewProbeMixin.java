package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.WindowPlacement;
import com.salts_inventory_update.controlify.qa.QaVisualFeatureProbe;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.salts_inventory_update.controlify.render.WorkspacePreviewRenderer", remap = false)
public abstract class FeatureWorkspacePreviewProbeMixin {
    @Inject(method = "renderWindowIcon", at = @At("HEAD"), remap = false)
    private static void iconBefore(UUID id, ItemStack icon, WindowPlacement placement, DesktopWindowBounds bounds,
                                    GuiGraphicsExtractor graphics, CallbackInfo callback) {
        QaVisualFeatureProbe.iconBefore(id,icon,placement,bounds);
    }
    @Inject(method = "renderWindowIcon", at = @At("RETURN"), remap = false)
    private static void iconAfter(UUID id, ItemStack icon, WindowPlacement placement, DesktopWindowBounds bounds,
                                   GuiGraphicsExtractor graphics, CallbackInfo callback) { QaVisualFeatureProbe.iconAfter(); }
    @Inject(method = "renderTuck", at = @At("HEAD"), remap = false)
    private static void tuckBefore(GuiGraphicsExtractor graphics, WindowPlacement placement, CallbackInfo callback) { QaVisualFeatureProbe.tuckBefore(placement); }
    @Inject(method = "renderTuck", at = @At("RETURN"), remap = false)
    private static void tuckAfter(GuiGraphicsExtractor graphics, WindowPlacement placement, CallbackInfo callback) { QaVisualFeatureProbe.tuckAfter(); }
}
