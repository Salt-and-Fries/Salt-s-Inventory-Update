package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A successful native upgrade closes its detached owner, not the vanilla menu. */
@Pseudo
@Mixin(targets="elocindev.prominentbase.client.gui.PrimordialForgeUpgradeScreenHandler",remap=false)
public abstract class ProminentUpgradeCloseMixin {
    @Unique private boolean salts_inventory_compat$upgradeComplete;

    @Redirect(method="lambda$new$0",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;closeContainer()V",remap=true))
    private void salts_inventory_compat$closeUpgrade(ServerPlayer player){
        if(DesktopSessions.activeNetworkMenu(player).orElse(null)==(Object)this){
            // Core's ordinary validity sweep disposes this exact server session.
            salts_inventory_compat$upgradeComplete=true;
        }else player.closeContainer();
    }

    // The optional Fabric class overrides a Minecraft method: accept the named
    // development spelling and the original 1.20.1 intermediary spelling.
    @Inject(method={"stillValid","method_7597"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$finished(Player player,CallbackInfoReturnable<Boolean> ci){
        if(salts_inventory_compat$upgradeComplete)ci.setReturnValue(false);
    }
}
