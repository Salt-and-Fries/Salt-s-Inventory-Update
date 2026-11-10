package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.travelersbackpack.TravelersPackets;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.tiviacz.travelersbackpack.util.PacketDistributorHelper",remap=false)
public abstract class TravelersPacketSenderMixin {
    @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$hostedAction(@Coerce Object packet,CallbackInfo ci){if(TravelersPackets.send(packet))ci.cancel();}
}
