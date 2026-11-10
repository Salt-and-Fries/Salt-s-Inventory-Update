package com.salts_inventory_update.compat.ssn.mixin;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ssn.SsnClient;

/** The publisher's integer boxes have no narration message on older builds. */
@Pseudo
@Mixin(targets={"com.lothrazar.storagenetwork.block.cable.inputfilter.ScreenCableImportFilter",
    "com.lothrazar.storagenetwork.block.cable.export.ScreenCableExportFilter"},remap=false)
public abstract class SsnWidgetMessageMixin {
    @Inject(method={"init","m_7856_"},at=@At("TAIL"),remap=false,require=0)
    private void salts$emptyNarration(CallbackInfo ci){
        for(var child:((Screen)(Object)this).children())if(child instanceof AbstractWidget widget&&widget.getMessage()==null)widget.setMessage(Component.empty());
        SsnClient.trackWidgets((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)(Object)this);
    }
}
