package com.salts_inventory_update.mixin.client.mousetweaks;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.DesktopMouseTweaksHooks;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;
import java.util.List;

@Mixin(value = InventoryDesktopScreen.class, remap = false)
public abstract class MouseTweaksScreenMixin implements IMTModGuiContainer3Ex, DesktopMouseTweaksHooks {
    private InventoryDesktopScreen screen() { return (InventoryDesktopScreen) (Object) this; }
    @Override public boolean MT_isMouseTweaksDisabled() { return false; }
    @Override public boolean MT_isWheelTweakDisabled() { return false; }
    @Override public List<Slot> MT_getSlots() { return screen().mouseTweaksSlots(); }
    @Override public Slot MT_getSlotUnderMouse(double x, double y) { return screen().mouseTweaksSlotAt(x,y); }
    @Override public boolean MT_isCraftingOutput(Slot slot) { return screen().mouseTweaksCraftingOutput(slot); }
    @Override public boolean MT_isIgnored(Slot slot) { return screen().mouseTweaksIgnored(slot); }
    @Override public boolean MT_disableRMBDraggingFunctionality() { return screen().mouseTweaksDisableRightDrag(); }
    @Override public void MT_clickSlot(Slot slot, int button, ClickType input) { screen().mouseTweaksClick(slot,button,input); }
    @Override public boolean salts_inventory_update$mouseTweaksScroll(double x, double y, double amount) {
        return yalter.mousetweaks.Main.onMouseScrolled(screen(),x,y,amount);
    }
}
