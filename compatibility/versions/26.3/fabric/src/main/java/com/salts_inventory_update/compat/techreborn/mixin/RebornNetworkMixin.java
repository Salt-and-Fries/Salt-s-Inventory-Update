package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.common.network.NetworkManager",remap=false)
public abstract class RebornNetworkMixin {
    @Inject(method="sendToPlayer",at=@At("HEAD"),cancellable=true,remap=false) private static void salts$update(CustomPacketPayload payload,ServerPlayer player,CallbackInfo ci){if(TechRebornWindows.sendUpdate(payload,player))ci.cancel();}
}
