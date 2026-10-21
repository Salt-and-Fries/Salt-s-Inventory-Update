package com.salts_inventory_update.mixin.client.mousetweaks;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yalter.mousetweaks.MouseButton;

@Mixin(value = yalter.mousetweaks.Main.class, remap = false)
public abstract class MouseTweaksInputMixin {
    @org.spongepowered.asm.mixin.Shadow private static boolean canDoLMBDrag;
    @org.spongepowered.asm.mixin.Shadow private static boolean canDoRMBDrag;
    @org.spongepowered.asm.mixin.Shadow private static boolean rmbTweakLeftOriginalSlot;
    @org.spongepowered.asm.mixin.Shadow private static net.minecraft.world.inventory.Slot oldSelectedSlot;
    @org.spongepowered.asm.mixin.Shadow private static double accumulatedScrollDelta;

    private static boolean salts_inventory_update$paused(InventoryDesktopScreen desktop) {
        if (!desktop.mouseTweaksDisabled()) return false;
        canDoLMBDrag=false;canDoRMBDrag=false;rmbTweakLeftOriginalSlot=false;
        oldSelectedSlot=null;accumulatedScrollDelta=0;
        return true;
    }
    @Inject(method = {"onMouseClicked", "onMouseReleased", "onMouseDrag"}, at = @At("HEAD"), cancellable = true)
    private static void beginButton(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> ci) {
        if (screen instanceof InventoryDesktopScreen desktop) {
            if (salts_inventory_update$paused(desktop)) {ci.setReturnValue(false);return;}
            desktop.mouseTweaksBegin();
        }
    }
    @Inject(method = {"onMouseClicked", "onMouseReleased", "onMouseDrag"}, at = @At("RETURN"))
    private static void endButton(Screen screen, double x, double y, MouseButton button, CallbackInfoReturnable<Boolean> ci) {
        if (screen instanceof InventoryDesktopScreen desktop) desktop.mouseTweaksEnd();
    }
    @Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
    private static void beginScroll(Screen screen, double x, double y, double delta, CallbackInfoReturnable<Boolean> ci) {
        if (screen instanceof InventoryDesktopScreen desktop) {
            if (salts_inventory_update$paused(desktop)) {ci.setReturnValue(false);return;}
            desktop.mouseTweaksBegin();
        }
    }
    @Inject(method = "onMouseScrolled", at = @At("RETURN"))
    private static void endScroll(Screen screen, double x, double y, double delta, CallbackInfoReturnable<Boolean> ci) {
        if (screen instanceof InventoryDesktopScreen desktop) desktop.mouseTweaksEnd();
    }
}
