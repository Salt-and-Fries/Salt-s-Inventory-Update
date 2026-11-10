package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import net.minecraft.server.level.ServerPlayer;
@Pseudo
@Mixin(targets={"com.github.glodblock.extendedae.network.EAENetworkServer", "com.github.glodblock.extendedae.network.EAENetworkClient", "com.glodblock.github.glodium.network.NetworkHandler"},remap=false)
public abstract class NetworkMixin {
 @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$action(@Coerce Object packet,CallbackInfo ci){if(ExtendedAePackets.clientSend(packet))ci.cancel();}
 @Inject(method="sendTo",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$update(@Coerce Object packet,ServerPlayer player,CallbackInfo ci){if(ExtendedAePackets.serverSend(packet,player))ci.cancel();}
}
