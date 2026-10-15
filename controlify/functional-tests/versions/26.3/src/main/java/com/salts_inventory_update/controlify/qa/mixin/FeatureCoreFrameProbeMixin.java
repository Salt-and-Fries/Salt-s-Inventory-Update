package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Proves decorative workspace chrome uses the same concrete Core renderer as ordinary menus. */
@Mixin(targets="com.salts_inventory_update.client.InventoryDesktopScreen",remap=false)
public abstract class FeatureCoreFrameProbeMixin {
    @Inject(method="renderSaltWindowFrame",at=@At("HEAD"),remap=false)
    private void coreFrame(GuiGraphicsExtractor graphics,int x,int y,int width,int height,String title,CallbackInfo callback) {
        QaContextualPresentationProbe.nativeFrameCore(x,y,width,height,title);
    }
}
