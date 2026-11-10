package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.GallerySelectionActions;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu",remap=false)
public abstract class GallerySelectionTransportMixin {
    @Redirect(method={"clickMenuButton","method_7604","m_6366_"},at=@At(value="INVOKE",target="Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V",remap=false))
    private void transport(@Coerce Object channel,Object packet) {
        if(GallerySelectionActions.send((AbstractContainerMenu)(Object)this))return;
        try{channel.getClass().getMethod("sendToServer",Object.class).invoke(channel,packet);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native Gallery transport changed",failure);}
    }
}
