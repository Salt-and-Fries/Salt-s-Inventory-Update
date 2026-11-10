package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.travelersbackpack.TravelersWindows;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.tiviacz.travelersbackpack.inventory.menu.AbstractBackpackMenu",remap=false)
public abstract class TravelersMenuMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$sourceIdentity(CallbackInfo ci){TravelersWindows.identity((AbstractContainerMenu)(Object)this);}
}
