package com.salts_inventory_update.compat.nativewindows.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional addon controls use the same authenticated lifetime as their AE window. */
@Pseudo
@Mixin(targets="com.almostreliable.merequester.platform.Platform",remap=false)
public abstract class RequesterNativePacketsMixin {
    private static boolean send(String name,Class<?>[] signature,Object... args) {
        try { return (boolean)Class.forName("com.salts_inventory_update.compat.ae2.nativeview.RequesterDesktop").getMethod(name,signature).invoke(null,args); }
        catch(ClassNotFoundException inactive){return false;}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Unsupported ME Requester action",failure);}
    }
    @Inject(method="sendRequestUpdate(JIZ)V",at=@At("HEAD"),cancellable=true)
    private static void state(long id,int slot,boolean enabled,CallbackInfo ci){
        if(send("state",new Class<?>[]{long.class,int.class,boolean.class},id,slot,enabled))ci.cancel();
    }
    @Inject(method="sendRequestUpdate(JIJJ)V",at=@At("HEAD"),cancellable=true)
    private static void numbers(long id,int slot,long batch,long stock,CallbackInfo ci){
        if(send("numbers",new Class<?>[]{long.class,int.class,long.class,long.class},id,slot,batch,stock))ci.cancel();
    }
    @Inject(method="sendDragAndDrop",at=@At("HEAD"),cancellable=true)
    private static void drop(long id,int slot,ItemStack stack,CallbackInfo ci){
        if(send("drop",new Class<?>[]{long.class,int.class,ItemStack.class},id,slot,stack))ci.cancel();
    }
}
