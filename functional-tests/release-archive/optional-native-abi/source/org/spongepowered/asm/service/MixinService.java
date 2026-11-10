package org.spongepowered.asm.service;
import java.lang.reflect.Proxy;
/** Only the bytecode-provider seam is synthetic; the plugin comes from its shipping jar. */
public final class MixinService {
    private static final IClassBytecodeProvider BYTECODE=(IClassBytecodeProvider)Proxy.newProxyInstance(
        MixinService.class.getClassLoader(),new Class<?>[]{IClassBytecodeProvider.class},(p,m,a)->{
            if(m.getName().equals("getClassNode"))return probe.OptionalAbiMain.fetch((String)a[0]);
            if(m.getDeclaringClass()==Object.class)return m.getName().equals("toString")?"synthetic-unit-bytecode-provider":0;
            throw new UnsupportedOperationException(m.toString());
        });
    private static final IMixinService SERVICE=(IMixinService)Proxy.newProxyInstance(
        MixinService.class.getClassLoader(),new Class<?>[]{IMixinService.class},(p,m,a)->{
            if(m.getName().equals("getBytecodeProvider"))return BYTECODE;
            if(m.getDeclaringClass()==Object.class)return m.getName().equals("toString")?"synthetic-unit-mixin-service":0;
            throw new UnsupportedOperationException(m.toString());
        });
    public static IMixinService getService(){return SERVICE;}
}
