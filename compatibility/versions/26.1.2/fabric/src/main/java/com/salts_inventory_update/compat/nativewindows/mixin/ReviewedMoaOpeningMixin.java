package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets={"com.aetherteam.protect_your_moa.capability.armor.MoaArmorCapability","com.aetherteam.protect_your_moa.attachment.MoaArmorAttachment"},remap=false)
public abstract class ReviewedMoaOpeningMixin {
    @Inject(method="openInventory",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$open(ServerPlayer player,@Coerce Object moa,CallbackInfo ci){
        try{
            var cargo=(Container)getClass().getMethod("getInventory").invoke(this);
            if(ReviewedEntityInventories.open("moa",(Entity)moa,player,cargo))ci.cancel();
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
}
