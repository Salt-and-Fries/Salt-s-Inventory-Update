package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 2000)
public abstract class AutomaticContainerScreenMixin {
    // Intercept the call site, including native subclasses that override renderSlot.
    // Capturing artwork must not bake item/count pixels into the retained background.
    @Redirect(method = "renderSlots", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;II)V"))
    private void salts_inventory_update$captureArtworkOnly(AbstractContainerScreen<?> screen, GuiGraphics graphics,
            Slot slot,int mouseX,int mouseY) {
        if (!AutomaticNativeScope.capturingSlots(screen))
            ((AutomaticContainerScreenAccessor)screen).salts_inventory_update$drawNativeSlot(graphics, slot,mouseX,mouseY);
    }
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hostTitle(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sharedCursor(GuiGraphics graphics, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sharedSlot(GuiGraphics graphics, Slot slot,int mouseX,int mouseY, CallbackInfo ci) {
        if (!AutomaticNativeScope.owns(this)) return;
        AutomaticNativeScope.renderSlot(this, graphics, slot); ci.cancel();
    }
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$singleSlotOwner(Slot slot, int id, int button, ClickType click, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
}
