package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Moonlight's empty native opening buffer needs the registered desktop envelope. */
@Pseudo
@Mixin(targets="net.mehvahdjukaar.supplementaries.common.entities.RedMerchantEntity",remap=false)
public abstract class RedMerchantOpeningMixin {
    // The containing vanilla override and its Minecraft parameter descriptors
    // are intermediary names in published Fabric jars. Moonlight's API owner
    // and method name are stable in both namespaces. There is exactly one
    // such opening call in this merchant; keep that constraint explicit.
    @ModifyArgs(method="*",at=@At(value="INVOKE",target="Lnet/mehvahdjukaar/moonlight/api/platform/PlatHelper;openCustomMenu",remap=false),remap=false,require=1,allow=1)
    private void salts_inventory_compat$openingEnvelope(Args args) {
        ServerPlayer player=args.get(0);
        if(DesktopSessions.shouldCapture(player))args.set(2,(Consumer<FriendlyByteBuf>)buffer->buffer.writeByte(0));
    }
}
