package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.quark.QuarkClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="org.violetmoon.quark.addons.oddities.client.screen.MatrixEnchantingScreen",remap=false)
public abstract class QuarkMatrixTransportMixin {
    @Redirect(method="send",at=@At(value="INVOKE",target="Lorg/violetmoon/zeta/client/ZetaClient;sendToServer(Lorg/violetmoon/zeta/network/IZetaMessage;)V",remap=false))
    private void addressed(@Coerce Object channel,@Coerce Object packet) { if(!QuarkClient.send(((AbstractContainerScreen<?>)(Object)this).getMenu(),packet))QuarkClient.originalSend(channel,packet); }
}
