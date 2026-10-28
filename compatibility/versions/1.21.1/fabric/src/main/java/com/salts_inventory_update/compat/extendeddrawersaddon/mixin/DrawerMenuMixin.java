package com.salts_inventory_update.compat.extendeddrawersaddon.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The upstream upgrade menu is always-valid; a persistent desktop must retain its actual source. */
@Pseudo
@Mixin(targets="net.extendeddrawersaddon.screen.DrawerScreenHandler",remap=false)
abstract class DrawerMenuMixin {
    @Shadow(remap=false) @Final private BlockPos pos;
    @Shadow(remap=false) @Final private Level world;
    @Unique private BlockEntity salts$source;
    @Inject(method={"stillValid","method_7597"},at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void salts$validateDesktopSource(Player player,CallbackInfoReturnable<Boolean> result) {
        var menu=(AbstractContainerMenu)(Object)this;
        if(!result.getReturnValueZ()||!DesktopSessions.hasOpenSessionMatching(player,m->m==menu))return;
        var source=pos==null?null:world.getBlockEntity(pos);
        if(salts$source==null)salts$source=source;
        result.setReturnValue(source!=null&&source==salts$source&&!source.isRemoved()&&world==player.level()
            &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64);
    }
}
