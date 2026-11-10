package com.salts_inventory_update.compat.ae2.extended.mixin;
import appeng.menu.AEBaseMenu;
import appeng.core.network.ClientboundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
/** Only Extended AE's sibling PAT screens use this addon lifetime route. */
@Mixin(value=AEBaseMenu.class,remap=false)
public abstract class NativePatternPacketsMixin {
 @Inject(method="sendPacketToClient",at=@At("HEAD"),cancellable=true,remap=false)
 private void update(ClientboundPacket packet,CallbackInfo ci){if(ExtendedAePackets.serverPatternUpdate(this,packet))ci.cancel();}
}
