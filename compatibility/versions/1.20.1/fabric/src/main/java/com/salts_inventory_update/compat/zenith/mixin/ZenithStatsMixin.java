package com.salts_inventory_update.compat.zenith.mixin;

import com.salts_inventory_update.compat.zenith.ZenithWindows;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.ench.table.StatsMessage",remap=false)
public abstract class ZenithStatsMixin {
    @Inject(method="sendTo",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$sessionStats(@Coerce Object stats,Player player,CallbackInfo ci){if(ZenithWindows.sendStats(stats,player))ci.cancel();}
}
