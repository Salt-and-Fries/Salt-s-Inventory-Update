package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.aether.AetherEquipmentClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native cloud button uses the same attached panel, retaining the original when SIU is unavailable. */
@Pseudo @Mixin(targets="com.aetherteam.aether.client.gui.component.inventory.AccessoryButton",remap=false)
public abstract class AetherCloudLauncherMixin {
    @Inject(method="lambda$new$0",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$equipment(@Coerce Object parent,@Coerce Object button,CallbackInfo ci){if(AetherEquipmentClient.openFromNative())ci.cancel();}
}
