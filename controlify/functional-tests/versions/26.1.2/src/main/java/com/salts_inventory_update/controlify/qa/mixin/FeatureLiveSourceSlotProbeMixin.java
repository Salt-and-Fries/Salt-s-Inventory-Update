package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.salts_inventory_update.client.InventoryDesktopScreen",remap=false)
public abstract class FeatureLiveSourceSlotProbeMixin {
    @Inject(method="renderSlot",at=@At("HEAD"),remap=false)
    private void slot(GuiGraphicsExtractor graphics,Slot slot,int x,int y,int mouseX,int mouseY,CallbackInfo callback) {
        QaContextualPresentationProbe.slot(slot);
    }
}
