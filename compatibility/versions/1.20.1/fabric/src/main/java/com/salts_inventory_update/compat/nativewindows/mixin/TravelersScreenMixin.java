package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.travelersbackpack.TravelersWindows;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.tiviacz.travelersbackpack.client.screens.AbstractBackpackScreen",remap=false)
public abstract class TravelersScreenMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){TravelersWindows.remember((AbstractContainerScreen<?>)(Object)this);}
}
