package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;

/** This Fabric release draws tab plates in Tab, before the later GuiHelper extraction existed. */
@Mixin(Tab.class)
public abstract class GuiHelperTabBackgroundMixin {
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$replaceHostedTabBackground(GuiGraphics graphics,
            Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        if (SophisticatedHostedScreenBridge.useSaltTabBackgrounds()) ci.cancel();
    }
}
