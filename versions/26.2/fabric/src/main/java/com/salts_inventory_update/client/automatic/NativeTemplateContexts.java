package com.salts_inventory_update.client.automatic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import com.salts_inventory_update.api.client.desktop.*;

/** Consumer callbacks see only public interfaces and their own state, never the host's state. */
final class NativeTemplateContexts {
    private NativeTemplateContexts() { }
    @SuppressWarnings("unchecked")
    static <C> C view(DesktopWindowContext<?,?> context,Object state,DesktopNativeView nativeView) {
        return (C)Proxy.newProxyInstance(DesktopNativeContext.class.getClassLoader(),new Class<?>[]{
            DesktopNativeContext.class,DesktopRenderContext.class,DesktopInputContext.class,DesktopSlotContext.class},(proxy,method,args)->{
                if(method.getName().equals("state")&&method.getParameterCount()==0)return state;
                if(method.getName().equals("nativeView")&&method.getParameterCount()==0)return nativeView;
                if(method.getName().equals("equals")&&method.getParameterCount()==1)return proxy==args[0];
                if(method.getName().equals("hashCode")&&method.getParameterCount()==0)return System.identityHashCode(proxy);
                if(method.getName().equals("toString")&&method.getParameterCount()==0)return "DesktopNativeContext["+nativeView.screenType()+"]";
                try{return method.invoke(context,args);}
                catch(InvocationTargetException failure){throw failure.getCause();}
            });
    }
}
