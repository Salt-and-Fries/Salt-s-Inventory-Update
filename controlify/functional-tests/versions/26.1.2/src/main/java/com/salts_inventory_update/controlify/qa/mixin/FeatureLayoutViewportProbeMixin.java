package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.RadialInventoryScreen;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import com.salts_inventory_update.controlify.qa.QaNativeGuideProbe;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads the real guide-reduced viewport; never samples or changes the production animator. */
@Mixin(value = SaltsControlifyClient.class, remap = false)
public abstract class FeatureLayoutViewportProbeMixin {
    @Inject(method = "animatedLayout", at = @At("HEAD"), remap = false)
    private void viewport(int width, int height, CallbackInfoReturnable<WorkspaceLayout<UUID>> callback) {
        if (Minecraft.getInstance().screen instanceof RadialInventoryScreen) {
            QaNativeGuideProbe.layoutWidth = width; QaNativeGuideProbe.layoutHeight = height; QaNativeGuideProbe.layoutFrames++;
        }
    }
}
