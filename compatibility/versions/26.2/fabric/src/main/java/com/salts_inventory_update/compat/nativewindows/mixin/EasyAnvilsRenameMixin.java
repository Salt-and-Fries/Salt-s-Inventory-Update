package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.EasyAnvilsRenameClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the normalized native text after its own prediction and formatting. */
@Pseudo
@Mixin(targets="fuzs.easyanvils.common.client.gui.screens.inventory.ModAnvilScreen",remap=false)
abstract class EasyAnvilsRenameMixin {
    @Inject(method="onNameChanged(Ljava/lang/String;)V",at=@At("TAIL"),remap=false)
    private void salts$rememberLatestName(String name,CallbackInfo ci){
        EasyAnvilsRenameClient.changed(((AbstractContainerScreen<?>)(Object)this).getMenu(),name);
    }
}
