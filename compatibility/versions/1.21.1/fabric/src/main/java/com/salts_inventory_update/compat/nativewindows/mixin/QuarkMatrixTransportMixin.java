package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.quark.QuarkClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.client.screen.MatrixEnchantingScreen",remap=false)
public abstract class QuarkMatrixTransportMixin {
    @Redirect(method="send",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",remap=false))
    private void addressed(CustomPacketPayload packet,CustomPacketPayload[] additional) {
        if(!QuarkClient.send(((AbstractContainerScreen<?>)(Object)this).getMenu(),packet))QuarkClient.originalSend(packet,additional);
    }
}
