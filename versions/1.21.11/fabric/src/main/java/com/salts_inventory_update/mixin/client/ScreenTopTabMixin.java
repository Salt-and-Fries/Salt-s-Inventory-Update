package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.DesktopTopTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The final Screen extraction entry point is shared by vanilla and NeoForge's screen hooks. */
@Mixin(Screen.class)
public abstract class ScreenTopTabMixin {
    @Unique
    private boolean salts_inventory_update$tabCoversPointer() {
        var mc = Minecraft.getInstance();
        return mc.screen == (Object)this && DesktopTopTab.covers(mc,
            mc.mouseHandler.getScaledXPos(mc.getWindow()), mc.mouseHandler.getScaledYPos(mc.getWindow()));
    }

    @ModifyVariable(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int salts_inventory_update$maskTabMouseX(int value) {
        return this.salts_inventory_update$tabCoversPointer() ? -10000 : value;
    }

    @ModifyVariable(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int salts_inventory_update$maskTabMouseY(int value) {
        return this.salts_inventory_update$tabCoversPointer() ? -10000 : value;
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
    private void salts_inventory_update$extractTab(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.screen == (Object)this) DesktopTopTab.extract(mc, GuiGraphicsExtractor.wrap(graphics),
            (int)mc.mouseHandler.getScaledXPos(mc.getWindow()), (int)mc.mouseHandler.getScaledYPos(mc.getWindow()));
    }
}
