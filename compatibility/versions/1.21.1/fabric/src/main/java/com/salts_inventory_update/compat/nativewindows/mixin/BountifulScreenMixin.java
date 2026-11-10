package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.bountiful.BountifulWindows;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="io.ejekta.bountiful.client.BoardScreen",remap=false)
public abstract class BountifulScreenMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){BountifulWindows.remember((AbstractContainerScreen<?>)(Object)this);}
}
