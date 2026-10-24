package com.salts_inventory_update.compat.trinkets.mixin;

import com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Brackets the upstream 3.7.2 native rebuild, through Core's supported managed-slot API. */
@Mixin(value = InventoryMenu.class, priority = 800)
public abstract class TrinketsPlayerMenuMixin {
    @Shadow @Final private Player owner;
    @Inject(method = "trinkets$updateTrinketSlots", at = @At("HEAD"), remap = false)
    private void salts$beforeRebuild(boolean changed, CallbackInfo ci) {
        DesktopPlayerMenuApi.beforeSlotRebuild((InventoryMenu) (Object) this);
    }
    @Inject(method = "trinkets$updateTrinketSlots", at = @At("RETURN"), remap = false)
    private void salts$afterRebuild(boolean changed, CallbackInfo ci) {
        DesktopPlayerMenuApi.afterSlotRebuild((InventoryMenu) (Object) this, owner);
    }
}
