package com.salts_inventory_update.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

import com.salts_inventory_update.inventory.InventoryExpansion;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void salts_inventory_update$appendExpansionSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        InventoryExpansion.appendMissingMenuSlots((InventoryMenu) (Object) this, owner);
    }
}
