package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.controlify.qa.QaHoverPacketProbe;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Fault injection is opt-in QA code and never enters a production JAR. */
@Mixin(value=DesktopContainerClient.class,remap=false)
public abstract class FeatureHoverPacketProbeMixin {
    @ModifyVariable(method="send",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static CustomPacketPayload outgoing(CustomPacketPayload payload) {
        return QaHoverPacketProbe.outgoing(payload);
    }
}
