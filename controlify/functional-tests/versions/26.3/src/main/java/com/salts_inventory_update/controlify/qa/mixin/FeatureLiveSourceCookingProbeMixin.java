package com.salts_inventory_update.controlify.qa.mixin;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.controlify.qa.QaContextualPresentationProbe;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.salts_inventory_update.client.InventoryDesktopScreen$FurnaceDesktopWindowDefinition",remap=false)
public abstract class FeatureLiveSourceCookingProbeMixin {
    @Inject(method="render",at=@At("HEAD"),remap=false)
    private void furnace(DesktopRenderContext<?,?> context,CallbackInfo callback) {
        if(context.model() instanceof AbstractFurnaceMenu menu) QaContextualPresentationProbe.cooking(menu);
    }
}
