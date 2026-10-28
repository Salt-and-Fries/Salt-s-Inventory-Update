package com.salts_inventory_update.compat.feedingtrough.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Patch the upstream always-valid menu only while Salt owns this exact desktop session. */
@Pseudo
@Mixin(targets = "slexom.animal_feeding_trough.platform.common.world.inventory.FeedingTroughMenu", remap = false)
abstract class FeedingTroughMenuMixin {
    // Named development, Fabric intermediary and Forge SRG names of the same 1.20.1 override.
    @Inject(method = {"stillValid", "method_7597", "m_6875_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void salts$validateDesktopSource(Player player, CallbackInfoReturnable<Boolean> result) {
        var menu = (AbstractContainerMenu) (Object) this;
        if (!result.getReturnValueZ() || !DesktopSessions.hasOpenSessionMatching(player, candidate -> candidate == menu)) return;
        result.setReturnValue(!menu.slots.isEmpty()
            && menu.getSlot(0).container instanceof BlockEntity source
            && !source.isRemoved() && source.getLevel() == player.level()
            && Container.stillValidBlockEntity(source, player));
    }
}
