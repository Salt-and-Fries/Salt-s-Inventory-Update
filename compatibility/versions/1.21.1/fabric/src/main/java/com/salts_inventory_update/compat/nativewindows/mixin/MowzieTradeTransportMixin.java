package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.MowzieTradeActions;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets={"com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade","com.bobmowzie.mowziesmobs.client.gui.GuiUmvuthiTrade"},remap=false)
public abstract class MowzieTradeTransportMixin {
    @Redirect(method="actionPerformed",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",remap=false))
    private void transport(CustomPacketPayload packet,CustomPacketPayload[] additional) {
        var menu=((AbstractContainerScreen<?>)(Object)this).getMenu();
        if(MowzieTradeActions.send(menu))return;
        try{Class.forName("net.neoforged.neoforge.network.PacketDistributor")
            .getMethod("sendToServer",CustomPacketPayload.class,CustomPacketPayload[].class).invoke(null,packet,additional);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native trade transport changed",failure);}
    }
}
