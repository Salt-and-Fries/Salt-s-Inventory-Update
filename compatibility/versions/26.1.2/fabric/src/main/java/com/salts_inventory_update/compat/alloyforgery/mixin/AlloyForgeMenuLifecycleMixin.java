package com.salts_inventory_update.compat.alloyforgery.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="io.wispforest.alloyforgery.AlloyForgeScreenHandler",remap=false)
abstract class AlloyForgeMenuLifecycleMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void salts$attachNativePlayer(int id,Inventory inventory,@Coerce Object forge,CallbackInfo ci){
        try{getClass().getMethod("owo$attachToPlayer",Player.class).invoke(this,inventory.player);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Alloy native menu attachment failed",failure);}
    }
}
