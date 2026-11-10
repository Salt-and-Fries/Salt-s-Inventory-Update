package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.MowzieTradeActions;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets={"com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade","com.bobmowzie.mowziesmobs.client.gui.GuiUmvuthiTrade"},remap=false)
public abstract class MowzieTradeTransportMixin {
    @Redirect(method="actionPerformed",at=@At(value="INVOKE",target="Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V",remap=false))
    private void transport(@Coerce Object channel,Object packet) {
        var menu=((AbstractContainerScreen<?>)(Object)this).getMenu();
        if(MowzieTradeActions.send(menu))return;
        try{channel.getClass().getMethod("sendToServer",Object.class).invoke(channel,packet);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native trade transport changed",failure);}
    }
}
