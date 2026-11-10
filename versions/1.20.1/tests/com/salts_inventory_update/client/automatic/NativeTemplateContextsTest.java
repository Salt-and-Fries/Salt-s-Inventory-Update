package com.salts_inventory_update.client.automatic;

import com.salts_inventory_update.api.client.desktop.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

class NativeTemplateContextsTest {
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @Test void nativeSourceMappingIsAvailableWithoutLeakingOrReflectingTheHostState() {
        var parent=(DesktopWindowContext<?,?>)Proxy.newProxyInstance(DesktopWindowContext.class.getClassLoader(),
            new Class<?>[]{DesktopWindowContext.class},(p,m,args)->{throw new AssertionError("Unexpected parent reflection: "+m);});
        var view=new DesktopNativeView("screen",new DesktopWindowBounds(0,0,176,166),new DesktopWindowBounds(7,16,162,56),List.of(),List.of());
        Object consumerState=new Object();
        DesktopNativeContext<?,?> context=NativeTemplateContexts.view(parent,consumerState,view,
            (x,y)->Optional.of(new DesktopNativeLayout.Point("pager",x-200,y+40)));
        assertSame(consumerState,context.state());assertSame(view,context.nativeView());
        var source=context.nativeSourceAt(215,10).orElseThrow();
        assertEquals(15,source.x());assertEquals(50,source.y());assertEquals("pager",source.regionId());
    }
}
