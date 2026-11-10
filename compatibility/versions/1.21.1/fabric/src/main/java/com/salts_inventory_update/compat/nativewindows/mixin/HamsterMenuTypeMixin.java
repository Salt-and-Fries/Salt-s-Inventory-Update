package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.SpawnInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="com.ninni.spawn.client.inventory.HamsterInventoryMenu",remap=false)
public abstract class HamsterMenuTypeMixin extends AbstractContainerMenu {
    protected HamsterMenuTypeMixin(MenuType<?> type,int id){super(type,id);}
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lcom/ninni/spawn/server/entity/mob/mammal/Hamster;)V",at=@At("RETURN"),remap=false)
    private void salts_inventory_compat$owner(int id,Inventory inventory,Container cargo,@Coerce Object owner,CallbackInfo ci){
        SpawnInventoryBridge.rememberOwner(this,(Entity)owner);
    }
    @Override public MenuType<?> getType(){
        var bridge=SpawnInventoryBridge.type();
        return bridge!=null?bridge:super.getType();
    }
    @Inject(method={"stillValid","method_7597","m_6875_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void salts_inventory_compat$sourceValid(Player player,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()&&!SpawnInventoryBridge.valid(this,player))cir.setReturnValue(false);
    }
}
