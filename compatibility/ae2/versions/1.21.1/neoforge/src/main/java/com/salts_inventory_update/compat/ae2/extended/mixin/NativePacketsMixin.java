package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import appeng.core.network.ServerboundPacket;
import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
@Mixin(targets="net.neoforged.neoforge.network.PacketDistributor",remap=false)
public abstract class NativePacketsMixin {
 @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
 private static void action(CustomPacketPayload packet,CustomPacketPayload[] additional,CallbackInfo ci){
  if(additional.length==0&&packet instanceof ServerboundPacket nativePacket&&MenuPacketScope.current() instanceof AEBaseMenu menu&&ExtendedAePackets.supports(menu)&&menu.desktopSyncId()!=null){menu.sendPacketToServer(nativePacket);ci.cancel();}
 }
}
