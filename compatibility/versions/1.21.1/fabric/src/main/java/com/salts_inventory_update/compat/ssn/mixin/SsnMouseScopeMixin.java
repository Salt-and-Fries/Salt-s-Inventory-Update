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
    @WrapMethod(method={"mouseClicked","m_6375_"},remap=false,require=0)
    private boolean salts$nativeOwner(double x,double y,int button,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(x,y,button));}
    @WrapMethod(method={"mouseScrolled","m_6050_"},remap=false,require=0)
    private boolean salts$nativeScroll(double x,double y,double horizontal,double amount,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(x,y,horizontal,amount));}
}
