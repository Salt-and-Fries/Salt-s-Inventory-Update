package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.aether.AetherEquipmentClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respect Aether's key/config guards; replace only its actual open request. */
@Pseudo @Mixin(targets="com.aetherteam.aether.client.event.hooks.GuiHooks",remap=false)
public abstract class AetherKeyLauncherMixin {
    @Inject(method="openAccessoryMenu",at=@At(value="NEW",target="com/aetherteam/aether/network/packet/serverbound/OpenAccessoriesPacket"),cancellable=true,remap=false)
    private static void salts$equipmentKey(CallbackInfo ci){if(AetherEquipmentClient.openFromNative())ci.cancel();}
}
