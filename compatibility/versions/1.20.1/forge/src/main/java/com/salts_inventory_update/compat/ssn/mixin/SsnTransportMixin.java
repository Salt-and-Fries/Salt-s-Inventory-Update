package com.salts_inventory_update.compat.ssn.mixin;

import com.salts_inventory_update.compat.ssn.*;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact publisher message classes only; other channels keep their native transport. */
@Mixin(value=SimpleChannel.class,remap=false)
public abstract class SsnTransportMixin {
    @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$ownedInput(Object message,CallbackInfo ci){if(SsnClient.send(message))ci.cancel();}
    @Inject(method="sendTo",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$ownedUpdate(Object message,Connection connection,NetworkDirection direction,CallbackInfo ci){
        if(direction==NetworkDirection.PLAY_TO_CLIENT&&connection.getPacketListener()instanceof ServerGamePacketListenerImpl listener&&SsnWindows.send(listener.player,message))ci.cancel();
    }
}
