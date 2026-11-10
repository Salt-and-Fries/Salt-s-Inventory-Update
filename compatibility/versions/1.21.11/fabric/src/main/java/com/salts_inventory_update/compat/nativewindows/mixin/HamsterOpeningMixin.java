package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.SpawnInventoryBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.ninni.spawn.entity.Hamster",remap=false)
public abstract class HamsterOpeningMixin {
    @Inject(method={"openCustomInventoryScreen","method_6722"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$desktop(Player player,CallbackInfo ci){
        if(SpawnInventoryBridge.open((Entity)(Object)this,player))ci.cancel();
    }
}
