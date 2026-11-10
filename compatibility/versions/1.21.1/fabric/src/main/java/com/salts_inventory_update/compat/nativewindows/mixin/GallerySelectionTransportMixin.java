package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.GallerySelectionActions;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu",remap=false)
public abstract class GallerySelectionTransportMixin {
    @Redirect(method={"clickMenuButton","method_7604","m_6366_"},at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",remap=false))
    private void transport(CustomPacketPayload packet,CustomPacketPayload[] additional) {
        if(GallerySelectionActions.send((AbstractContainerMenu)(Object)this))return;
        try{Class.forName("net.neoforged.neoforge.network.PacketDistributor").getMethod("sendToServer",CustomPacketPayload.class,CustomPacketPayload[].class).invoke(null,packet,additional);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native Gallery transport changed",failure);}
    }
}
