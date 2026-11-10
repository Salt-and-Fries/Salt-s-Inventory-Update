package com.salts_inventory_update.compat.toms_storage;
import java.lang.reflect.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TomsSidebarHitTest {
    private static Class<?> owner() throws Exception { return Class.forName("com.salts_inventory_update.compat.toms_storage.client.TomsStorageDesktopWindows"); }
    private static int hit(double x,double y,int count) throws Exception {
        Method method=owner().getDeclaredMethod("buttonIndex",double.class,double.class,int.class,int.class,int.class);
        method.setAccessible(true);return (Integer)method.invoke(null,x,y,100,50,count);
    }
    private static int mouse(double x,double y,int count) throws Exception {
        Method method=java.util.Arrays.stream(owner().getDeclaredMethods()).filter(m->m.getName().equals("buttonIndex")&&m.getParameterCount()==4).findFirst().orElseThrow();
        Class<?> eventType=method.getParameterTypes()[0];Object event=null;
        int button=0;
        try {Field field=Class.forName("com.mojang.blaze3d.platform.InputConstants").getDeclaredField("MOUSE_BUTTON_LEFT");field.setAccessible(true);button=field.getInt(null);}catch(NoSuchFieldException legacy){}
        for(Constructor<?> ctor:eventType.getDeclaredConstructors())if(ctor.getParameterCount()==3){
            Class<?> info=ctor.getParameterTypes()[2];Object argument=info==int.class?button:info.getDeclaredConstructor(int.class,int.class).newInstance(button,0);
            ctor.setAccessible(true);event=ctor.newInstance(x,y,argument);break;
        }
        assertNotNull(event);method.setAccessible(true);return (Integer)method.invoke(null,event,100,50,count);
    }
    @ParameterizedTest @ValueSource(ints={2,3,5}) void hoverMatchesOnlyTheRenderedVerticalRail(int count) throws Exception {
        for(int row=0;row<count;row++)assertEquals(row,hit(108,58+18*row,count));
        for(int phantom=1;phantom<count;phantom++)assertEquals(-1,hit(108+18*phantom,58,count),"Unpainted horizontal controls must not cover filter slots");
    }
    @ParameterizedTest @ValueSource(ints={2,3,5}) void actualMouseClicksLeaveAdjacentSlotCentersAlone(int count) throws Exception {
        for(int row=0;row<count;row++)assertEquals(row,mouse(108,58+18*row,count));
        for(int phantom=1;phantom<count;phantom++)assertEquals(-1,mouse(108+18*phantom,58,count));
    }
    @ParameterizedTest @ValueSource(ints={2,3,5}) void gapsAndOuterBordersDoNotActivateButtons(int count) throws Exception {
        assertEquals(-1,hit(99,58,count));assertEquals(-1,hit(116,58,count));
        assertEquals(-1,hit(108,49,count));assertEquals(-1,hit(108,66,count));
        assertEquals(-1,hit(108,67,count));assertEquals(-1,hit(108,50+18*(count-1)+16,count));
    }
}
