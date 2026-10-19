package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import dev.isxander.controlify.Controlify;
import java.lang.reflect.Field;
import java.util.Collection;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disposable QA observation of actual native warp receipts and registered callbacks. */
@Mixin(value=NativeCursorWarpGuard.class,remap=false)
public abstract class QaCursorWarpTraceMixin {
    @Inject(method="nativeWarp",at=@At("HEAD"),remap=false)
    private static void saltsQA$nativeWarp(long window,double x,double y,Runnable warp,CallbackInfo callback){
        if(ControlifyGameplayHarness.tracingCursorWarp())System.out.println("CONTROLIFY_QA_WARP_NATIVE window="+window+" x="+x+" y="+y+" transition="+((ThreadLocal<?>)saltsQA$field(NativeCursorWarpGuard.class,"TRANSITION")).get()+" pending="+saltsQA$pending());
    }
    @Inject(method="nativeCursorMode",at=@At("HEAD"),remap=false)
    private static void saltsQA$cursorMode(long window,int mode,int value,Runnable change,CallbackInfo callback){
        if(ControlifyGameplayHarness.tracingCursorWarp())System.out.println("CONTROLIFY_QA_WARP_MODE window="+window+" mode="+mode+" value="+value+" pending="+saltsQA$pending());
    }
    @Inject(method="keyboardTransition",at=@At("RETURN"),remap=false)
    private static void saltsQA$transition(RadialInventoryScreen parent,Runnable transition,CallbackInfo callback){
        if(ControlifyGameplayHarness.tracingCursorWarp())System.out.println("CONTROLIFY_QA_WARP_TRANSITION vmouse="+Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()+" pending="+saltsQA$pending());
    }
    @Inject(method="wrap",at=@At("RETURN"),cancellable=true,remap=false)
    private static void saltsQA$callback(GLFWCursorPosCallbackI incoming,CallbackInfoReturnable<GLFWCursorPosCallbackI> callback){
        GLFWCursorPosCallbackI wrapped=callback.getReturnValue();
        if(wrapped==null)return;
        callback.setReturnValue((window,x,y)->{
            boolean trace=ControlifyGameplayHarness.tracingCursorWarp();
            if(trace)System.out.println("CONTROLIFY_QA_WARP_CALLBACK before window="+window+" x="+x+" y="+y+" vmouse="+Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()+" pending="+saltsQA$pending());
            wrapped.invoke(window,x,y);
            if(trace)System.out.println("CONTROLIFY_QA_WARP_CALLBACK after vmouse="+Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()+" matches="+saltsQA$field(NativeCursorWarpGuard.class,"matchedCallbacks")+" pending="+saltsQA$pending());
        });
    }
    @Unique private static String saltsQA$pending(){
        Object tracker=saltsQA$field(NativeCursorWarpGuard.class,"ECHOES");
        StringBuilder result=new StringBuilder();
        for(Object warp:(Collection<?>)saltsQA$field(tracker,"pending"))result.append("[owner=").append(saltsQA$field(warp,"owner")).append(",window=").append(saltsQA$field(warp,"window")).append(",x=").append(saltsQA$field(warp,"x")).append(",y=").append(saltsQA$field(warp,"y")).append(",ageMs=").append((System.nanoTime()-((Number)saltsQA$field(warp,"created")).longValue())/1_000_000).append(']');
        return result.toString();
    }
    @Unique private static Object saltsQA$field(Object owner,String name){
        try{Class<?> type=owner instanceof Class<?> value?value:owner.getClass();Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner instanceof Class<?>?null:owner);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
}
