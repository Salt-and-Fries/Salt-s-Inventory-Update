package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.controlify.qa.QaPlaytestProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryDesktopScreen.class)
public abstract class FeaturePlaytestCarriedMixin {
    @Inject(method = "renderCarried", at = @At("HEAD"), remap = false)
    private static void carried(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y,
            Minecraft minecraft, String count, CallbackInfo callback) {
        QaPlaytestProbe.carried(graphics, stack);
    }
}
