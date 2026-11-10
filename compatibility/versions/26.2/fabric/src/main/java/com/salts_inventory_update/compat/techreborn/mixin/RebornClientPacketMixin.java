package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking",remap=false)
public abstract class RebornClientPacketMixin {
    @Inject(method="send",at=@At("HEAD"),cancellable=true,remap=false) private static void salts$key(CustomPacketPayload packet,CallbackInfo ci){if(TechRebornScreens.send(packet))ci.cancel();}
}
