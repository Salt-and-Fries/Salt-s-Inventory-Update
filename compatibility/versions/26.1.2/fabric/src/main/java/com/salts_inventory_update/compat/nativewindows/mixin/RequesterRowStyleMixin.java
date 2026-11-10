package com.salts_inventory_update.compat.nativewindows.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.almostreliable.merequester.client.abstraction.AbstractRequesterScreen",remap=false)
public abstract class RequesterRowStyleMixin {
    @Inject(method="addSubWidget",at=@At("RETURN"))
    private void remember(String name,AbstractWidget widget,java.util.Map<?,?> widgets,CallbackInfo ci) {
        try { Class.forName("com.salts_inventory_update.compat.ae2.nativeview.AENativeWidgets")
            .getMethod("remember",Object.class,String.class,Object.class).invoke(null,this,name,widget); }
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Requester row geometry changed",failure);}
    }
}
