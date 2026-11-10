package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.client.ClientNetworkManager",remap=false)
public abstract class RebornClientPacketMixin {
    @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$keyAction(@Coerce Object packet,CallbackInfo ci){if(TechRebornScreens.send(packet))ci.cancel();}
}
