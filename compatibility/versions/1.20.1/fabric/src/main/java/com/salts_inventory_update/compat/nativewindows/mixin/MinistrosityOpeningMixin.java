package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture the native pet opening after its interaction/ownership checks. */
@Pseudo
@Mixin(targets="com.github.L_Ender.cataclysm.entity.Pet.Netherite_Ministrosity_Entity",remap=false)
public abstract class MinistrosityOpeningMixin {
    @Inject(method={"openCustomInventoryScreen","m_213583_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$open(Player player,CallbackInfo ci){
        var entity=(Entity)(Object)this;
        if(player instanceof ServerPlayer server&&entity.isAlive()
            &&ReviewedEntityInventories.open("ministrosity",entity,server,ReviewedEntityInventories.cargo(this,"miniInventory"))){
            ForeignMenuAccess.invoke(this,"setAttackState",new Class<?>[]{int.class},3);
            ci.cancel();
        }
    }
}
