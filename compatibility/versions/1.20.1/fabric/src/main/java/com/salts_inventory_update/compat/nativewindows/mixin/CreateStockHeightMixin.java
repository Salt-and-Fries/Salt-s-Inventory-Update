package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.create.CreateNativeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Only an explicitly hosted instance opts in; native-only screens are untouched. */
@Pseudo
@Mixin(targets="com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen",remap=false)
public abstract class CreateStockHeightMixin {
    @ModifyVariable(method={"init","method_25426","m_7856_"},at=@At("STORE"),index=1,require=0,remap=false)
    private int salts$compactNativeViewport(int value) { return CreateNativeAccess.compactHeight(this,value); }
}
