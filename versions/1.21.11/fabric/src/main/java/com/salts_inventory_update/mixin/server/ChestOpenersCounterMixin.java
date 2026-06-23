package com.salts_inventory_update.mixin.server;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(targets = "net.minecraft.world.level.block.entity.ChestBlockEntity$1")
public abstract class ChestOpenersCounterMixin {
    @Shadow(remap = false)
    @Final
    private ChestBlockEntity field_27211;

    @Inject(method = "isOwnContainer", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$desktopSessionOwnsChest(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DesktopContainerSessions.hasOpenSessionForContainer(player, (Container) this.field_27211)) {
            cir.setReturnValue(true);
        }
    }
}
