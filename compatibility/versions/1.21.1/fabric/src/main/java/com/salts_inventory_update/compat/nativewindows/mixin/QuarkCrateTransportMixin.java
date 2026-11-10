package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.quark.QuarkClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.inventory.CrateMenu",remap=false)
public abstract class QuarkCrateTransportMixin {
    @Redirect(method="scroll",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",remap=false))
    private void addressed(CustomPacketPayload packet,CustomPacketPayload[] additional) {
        if(!QuarkClient.send((AbstractContainerMenu)(Object)this,packet))QuarkClient.originalSend(packet,additional);
    }
}
