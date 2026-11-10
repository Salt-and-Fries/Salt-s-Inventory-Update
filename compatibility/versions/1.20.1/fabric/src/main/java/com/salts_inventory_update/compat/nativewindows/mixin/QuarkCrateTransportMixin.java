package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.quark.QuarkClient;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.inventory.CrateMenu",remap=false)
public abstract class QuarkCrateTransportMixin {
    @Redirect(method="scroll",at=@At(value="INVOKE",target="Lorg/violetmoon/zeta/client/ZetaClient;sendToServer(Lorg/violetmoon/zeta/network/IZetaMessage;)V",remap=false))
    private void addressed(@Coerce Object channel,@Coerce Object packet) { if(!QuarkClient.send((AbstractContainerMenu)(Object)this,packet))QuarkClient.originalSend(channel,packet); }
}
