package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tomstrading.TomsTradingWindows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.tom.trading.gui.VendingMachineConfigScreen",remap=false)
public abstract class TomsTradingConfigButtonMixin {
    @Inject(method="sendButtonClick",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$popupButton(int id,CallbackInfo ci){if(TomsTradingWindows.menuButton(this,id))ci.cancel();}
}
