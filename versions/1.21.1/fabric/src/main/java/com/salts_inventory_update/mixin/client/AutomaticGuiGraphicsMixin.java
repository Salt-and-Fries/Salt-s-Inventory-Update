package com.salts_inventory_update.mixin.client;

import java.util.List;
import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class AutomaticGuiGraphicsMixin {
    @Inject(method = "applyScissor", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$captureScissor(net.minecraft.client.gui.navigation.ScreenRectangle rectangle, CallbackInfo ci) {
        if (AutomaticNativeScope.captureScissor((GuiGraphics)(Object)this, rectangle)) ci.cancel();
    }
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int salts_inventory_update$scissorLeft(int value) { return AutomaticNativeScope.nativeScissor(value, true); }
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int salts_inventory_update$scissorTop(int value) { return AutomaticNativeScope.nativeScissor(value, false); }
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int salts_inventory_update$scissorRight(int value) { return AutomaticNativeScope.nativeScissor(value, true); }
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private int salts_inventory_update$scissorBottom(int value) { return AutomaticNativeScope.nativeScissor(value, false); }
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"))
    private void salts_inventory_update$observe(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                                 float u1, float u2, float v1, float v2, CallbackInfo ci) {
        AutomaticNativeScope.recordDraw((GuiGraphics)(Object)this, x1, x2, y1, y2);
        AutomaticNativeScope.beginTextureClip((GuiGraphics)(Object)this, x1, x2, y1, y2);
    }
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"))
    private void salts_inventory_update$observeColored(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
        float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha, CallbackInfo ci) {
        AutomaticNativeScope.recordDraw((GuiGraphics)(Object)this, x1, x2, y1, y2);
        AutomaticNativeScope.beginTextureClip((GuiGraphics)(Object)this, x1, x2, y1, y2);
    }
    @Inject(method = {"innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
                      "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V"}, at = @At("TAIL"))
    private void salts_inventory_update$restoreBodyClip(CallbackInfo ci) {
        AutomaticNativeScope.finishTextureClip(AutomaticNativeScope.current(), (GuiGraphics)(Object)this);
    }
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$deferTooltip(Font font, List<ClientTooltipComponent> components,
                                                     int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        var scope = AutomaticNativeScope.current();
        if (scope == null || scope.rendering() == null) return;
        scope.owner().tooltipFont = font; scope.owner().tooltip = List.copyOf(components); ci.cancel();
    }

}
