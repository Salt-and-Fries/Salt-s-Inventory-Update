package com.salts_inventory_update.compat.chipped.mixin;
import com.salts_inventory_update.compat.chipped.ChippedSources;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="earth.terrarium.chipped.common.menus.WorkbenchMenu",remap=false)
abstract class ChippedMenuMixin {
    @Inject(method="<init>",at=@At("TAIL"),remap=false)
    private void salts$source(CallbackInfo ci){ChippedSources.attach((AbstractContainerMenu)(Object)this);}
    @Inject(method={"stillValid","method_7597","m_6875_"},at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void salts$valid(Player player,CallbackInfoReturnable<Boolean> result){
        var source=ChippedSources.get((AbstractContainerMenu)(Object)this);
        if(source!=null&&player instanceof ServerPlayer server&&DesktopSessions.hasOpenSessionMatching(player,m->m==(Object)this))
            result.setReturnValue(result.getReturnValueZ()&&source.valid(server));
    }
}
