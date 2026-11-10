package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import appeng.core.sync.BasePacket;
import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
@Pseudo
// NetworkHandler is the shared interface in supported AE2 15.x builds.
// Mixin validates target kind before shouldApplyMixin, so the plugin cannot
// safely filter an interface out of a class mixin's target array.
@Mixin(targets="appeng.core.sync.network.ClientNetworkHandler",remap=false)
public abstract class ExtendedAeNativePacketsMixin {
 @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,require=0,remap=false) private void siu$native(BasePacket packet,CallbackInfo ci){if(MenuPacketScope.current() instanceof AEBaseMenu menu&&(ExtendedAePackets.supports(menu)||menu.getClass().getName().startsWith("com.almostreliable.merequester."))&&menu.desktopSyncId()!=null){menu.sendPacketToServer(packet);ci.cancel();}}
}
