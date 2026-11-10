package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.SpawnInventoryBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="com.ninni.spawn.client.inventory.HamsterInventoryMenu",remap=false)
public abstract class HamsterMenuTypeMixin extends AbstractContainerMenu {
    protected HamsterMenuTypeMixin(MenuType<?> type,int id){super(type,id);}
    @Override public MenuType<?> getType(){return SpawnInventoryBridge.type();}
    @Inject(method={"stillValid","method_7597","m_6875_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void salts_inventory_compat$sourceValid(Player player,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()&&!SpawnInventoryBridge.valid(this,player))cir.setReturnValue(false);
    }
}
