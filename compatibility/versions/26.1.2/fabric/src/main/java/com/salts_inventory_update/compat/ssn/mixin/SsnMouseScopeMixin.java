package com.salts_inventory_update.compat.ssn.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.compat.ssn.SsnClient;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets={"com.lothrazar.storagenetwork.block.AbstractNetworkScreen",
    "com.lothrazar.storagenetwork.block.cable.inputfilter.ScreenCableImportFilter",
    "com.lothrazar.storagenetwork.block.cable.export.ScreenCableExportFilter",
    "com.lothrazar.storagenetwork.block.cable.linkfilter.ScreenCableFilter",
    "com.lothrazar.storagenetwork.block.collection.ScreenCollectionFilter",
    "com.lothrazar.storagenetwork.block.cable.processing.ScreenCableProcess"},remap=false)
public abstract class SsnMouseScopeMixin {
    @WrapMethod(method="mouseClicked",remap=false,require=0)
    private boolean salts$nativeOwner(net.minecraft.client.input.MouseButtonEvent event,boolean twice,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event,twice));}
    @WrapMethod(method="mouseScrolled",remap=false,require=0)
    private boolean salts$nativeScroll(double x,double y,double horizontal,double amount,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(x,y,horizontal,amount));}
}
