package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tomstrading.TomsTradingWindows;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="com.tom.trading.gui.PlatformContainerScreen",remap=false)
public abstract class TomsTradingScreenMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){TomsTradingWindows.remember((AbstractContainerScreen<?>)(Object)this);}
}
