package com.salts_inventory_update.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.debug.DesktopDebug;
import org.spongepowered.asm.mixin.Unique;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Unique
    private static int salts_inventory_update$probeLogs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void salts_inventory_update$appendExpansionSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        if (salts_inventory_update$probeLogs < 8) {
            salts_inventory_update$probeLogs++;
            DesktopDebug.probe(
                "mixin InventoryMenuMixin applied owner={} active={} ownerClass={} menuSlotsBefore={}",
                owner.getName().getString(),
                active,
                owner.getClass().getName(),
                ((InventoryMenu) (Object) this).slots.size()
            );
        }
        InventoryExpansion.appendMissingMenuSlots((InventoryMenu) (Object) this, owner);
    }
}
