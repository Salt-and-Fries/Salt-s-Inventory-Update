package com.salts_inventory_update.spatial.mixin;
import com.salts_inventory_update.client.WindowedInventoryClient;
import com.salts_inventory_update.spatial.SpatialGuiIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;

@Mixin(value = WindowedInventoryClient.class, remap = false)
public class SpatialCoreHudMixin {
    @Inject(method = "shouldHideCrosshair", at = @At("HEAD"), cancellable = true)
    private static void salts$aimAtPanel(CallbackInfoReturnable<Boolean> cir) {
        if (SpatialGuiIntegration.active() && SpatialGUIRenderer.isCrosshairModeActive()) cir.setReturnValue(false);
    }
}
