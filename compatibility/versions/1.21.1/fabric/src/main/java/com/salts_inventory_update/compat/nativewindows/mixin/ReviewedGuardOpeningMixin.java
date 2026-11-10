package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="tallestegg.guardvillagers.common.entities.Guard",remap=false)
public abstract class ReviewedGuardOpeningMixin {
    @Shadow(remap=false) public boolean interacting;
    @Inject(method="openGui",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$open(ServerPlayer player,CallbackInfo ci){
        if(!DesktopSessions.shouldCapture(player))return;
        try{getClass().getMethod("setOwnerId",java.util.UUID.class).invoke(this,player.getUUID());}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        if(ReviewedEntityInventories.open("guard",(Entity)(Object)this,player,ReviewedEntityInventories.cargo(this,"guardInventory"))){interacting=true;ci.cancel();}
    }
}
