package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.bountiful.BountifulWindows;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only a hosted board adapts the upstream count renderer's absolute pose. */
@Pseudo
@Mixin(targets="io.ejekta.bountiful.kambrik.gui.KGuiDsl",remap=false)
public abstract class BountifulTextMixin {
    @Inject(method="textImmediate",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$bountyQuantities(int x,int y,Component text,CallbackInfo ci){if(BountifulWindows.immediateText(this,x,y,text))ci.cancel();}
}
