package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The hosted menu snapshot may arrive before this screen creates its button. */
@Pseudo
@Mixin(targets="com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade", remap=false)
abstract class SculptorButtonStateMixin {
    @Inject(method={"init", "m_7856_"}, at=@At("RETURN"), require=1)
    private void salts$refreshInitialEligibility(CallbackInfo ci) {
        var menu=((AbstractContainerScreen<?>)(Object)this).getMenu();
        var inventory=ForeignMenuAccess.invoke(menu,"getInventorySculptor");
        // Delegate payment and obstruction rules to the original mod.
        ForeignMenuAccess.invoke(this,"onChange",new Class<?>[]{Container.class},inventory);
    }
}
