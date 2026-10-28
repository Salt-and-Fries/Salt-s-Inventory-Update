package com.salts_inventory_update.compat.tieredz.mixin;

import com.salts_inventory_update.compat.tieredz.GearClientMenus;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hosted anvil input belongs to the API EditBox; prevent its background screen sending a raw rename. */
@Mixin(AnvilScreen.class)
abstract class HostedAnvilRenameMixin {
    @Inject(method="onNameChanged",at=@At("HEAD"),cancellable=true)
    private void salts$apiRenameOwner(String name,CallbackInfo callback) {
        if(GearClientMenus.ownsRename(((AbstractContainerScreen<?>)(Object)this).getMenu()))callback.cancel();
    }
}
