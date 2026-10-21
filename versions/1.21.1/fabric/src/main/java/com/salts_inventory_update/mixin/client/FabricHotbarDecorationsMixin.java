package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.WindowedInventoryClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric's vanilla HUD path; Forge/NeoForge use their independent overlay/layer events. */
@Mixin(value = Gui.class, priority = 1100)
public abstract class FabricHotbarDecorationsMixin {
    @Inject(method = { "renderHotbarAndDecorations", "renderExperienceLevel" }, at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hideHotbarDecorationsForDesktop(CallbackInfo ci) {
        if (WindowedInventoryClient.shouldHideHotbar()) {
            ci.cancel();
        }
    }
}
