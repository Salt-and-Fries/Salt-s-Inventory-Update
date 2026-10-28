package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
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
    @Redirect(method = "extractSlots", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V"))
    private void salts_inventory_update$captureArtworkOnly(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
            Slot slot,int mouseX,int mouseY) {
        if (!AutomaticNativeScope.capturingSlots(screen))
            ((AutomaticContainerScreenAccessor)screen).salts_inventory_update$drawNativeSlot(graphics, slot,mouseX,mouseY);
    }
    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$hostTitle(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
    @Inject(method = "extractFloatingItem", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sharedCursor(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$sharedSlot(GuiGraphicsExtractor graphics, Slot slot,int mouseX,int mouseY, CallbackInfo ci) {
        if (!AutomaticNativeScope.owns(this)) return;
        AutomaticNativeScope.renderSlot(this, graphics, slot); ci.cancel();
    }
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$singleSlotOwner(Slot slot, int id, int button, ContainerInput click, CallbackInfo ci) {
        if (AutomaticNativeScope.owns(this)) ci.cancel();
    }
}
