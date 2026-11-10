package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="aztech.modern_industrialization.client.machines.gui.MachineScreen",remap=false)
public abstract class ModernMachineScreenMixin {
    @Inject(method={"renderLabels","method_2388","m_280003_"},at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void salts$labels(GuiGraphics graphics,int x,int y,CallbackInfo ci){
        // Newer MI overrides the base label renderer. Suppress only that
        // duplicate title/Inventory text, keeping component artwork intact.
        if(DesktopNativeRenderTarget.current().isPresent())ci.cancel();
    }
    @Inject(method={"init","method_25426","m_7856_"},at=@At("RETURN"),require=0,remap=false)
    private void salts$controlRow(CallbackInfo ci){
        var screen=(AbstractContainerScreen<?>)(Object)this;
        if(ModernIndustrializationClient.hosted(screen.getMenu()))ModernIndustrializationClient.prepare(screen.getMenu());
    }
}
