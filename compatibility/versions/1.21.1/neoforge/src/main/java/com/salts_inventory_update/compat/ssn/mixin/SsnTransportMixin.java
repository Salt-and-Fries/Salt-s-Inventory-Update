package com.salts_inventory_update.compat.ssn.mixin;
import com.salts_inventory_update.compat.ssn.*;
import java.util.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PacketDistributor.class,remap=false)
public abstract class SsnTransportMixin {
 @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
 private static void salts$ownedInput(CustomPacketPayload first,CustomPacketPayload[] rest,CallbackInfo ci){
  var pending=new ArrayList<CustomPacketPayload>();boolean routed=SsnClient.send(first);if(!routed)pending.add(first);
  for(var p:rest)if(SsnClient.send(p))routed=true;else pending.add(p);
  if(routed){for(var p:pending)PacketDistributor.sendToServer(p);ci.cancel();}
 }
 @Inject(method="sendToPlayer",at=@At("HEAD"),cancellable=true,remap=false)
 private static void salts$ownedUpdate(ServerPlayer player,CustomPacketPayload first,CustomPacketPayload[] rest,CallbackInfo ci){
  var pending=new ArrayList<CustomPacketPayload>();boolean routed=SsnWindows.send(player,first);if(!routed)pending.add(first);
  for(var p:rest)if(SsnWindows.send(player,p))routed=true;else pending.add(p);
  if(routed){for(var p:pending)PacketDistributor.sendToPlayer(player,p);ci.cancel();}
 }
}
