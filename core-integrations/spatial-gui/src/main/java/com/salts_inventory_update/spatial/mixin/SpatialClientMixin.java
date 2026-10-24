package com.salts_inventory_update.spatial.mixin;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import com.salts_inventory_update.spatial.SpatialOptionsAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.SpatialGUI;

@Mixin(value = SpatialGUIClient.class, remap = false)
public class SpatialClientMixin {
    @Inject(method = "shouldHookScreen", at = @At("HEAD"), cancellable = true)
    private static void salts$desktop(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof InventoryDesktopScreen) cir.setReturnValue(
            !(SpatialGuiIntegration.mousePass && SpatialGuiIntegration.movement()) && SpatialGuiIntegration.eligible(screen));
    }
    @Inject(method = "shouldUseFirstPersonMode", at = @At("HEAD"), cancellable = true)
    private static void salts$viewMode(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (!(screen instanceof InventoryDesktopScreen d) || !SpatialGuiIntegration.eligible(d)) return;
        var c = SpatialGUI.config;
        cir.setReturnValue(c.autoDetectCameraMode
            ? ((SpatialOptionsAccess)Minecraft.getInstance().options).salts$perspective().isFirstPerson()
            : (SpatialGuiIntegration.containerWorkspace(d) ? c.firstPersonModeContainers : c.firstPersonModeInventory)
                || SpatialGUIClient.getSwitchedToFirstPersonDueToBlock());
    }
    @Inject(method = {"shouldHideHud", "shouldHideHotbar", "shouldHideCrosshair"}, at = @At("HEAD"), cancellable = true)
    private static void salts$gameplayHud(CallbackInfoReturnable<Boolean> cir) {
        if (SpatialGuiIntegration.gameplay()) cir.setReturnValue(false);
    }
}
