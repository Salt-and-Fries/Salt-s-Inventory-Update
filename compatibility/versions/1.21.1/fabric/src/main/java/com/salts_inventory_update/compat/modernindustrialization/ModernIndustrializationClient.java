package com.salts_inventory_update.compat.modernindustrialization;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Upstream-owned screens and addressed updates, without accessing Core internals. */
public final class ModernIndustrializationClient {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Map<Integer,WeakReference<AbstractContainerMenu>> MENUS=new HashMap<>();
    private static final ThreadLocal<Boolean> ROUTING=ThreadLocal.withInitial(()->false);
    private ModernIndustrializationClient() { }
    public static void remember(AbstractContainerScreen<?> screen){SCREENS.put(screen.getMenu(),new WeakReference<>(screen));}
    static void open(AbstractContainerMenu menu){MENUS.put(menu.containerId,new WeakReference<>(menu));prepare(menu);}
    static void close(AbstractContainerMenu menu){var ref=MENUS.get(menu.containerId);if(ref!=null&&ref.get()==menu)MENUS.remove(menu.containerId);SCREENS.remove(menu);}
    public static boolean hosted(AbstractContainerMenu menu){var ref=MENUS.get(menu.containerId);return ref!=null&&ref.get()==menu;}
    public static Object field(Object owner,String name){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        return null;
    }
    static String blockId(AbstractContainerMenu menu){
        var params=field(menu,"guiParams");var id=params==null?null:field(params,"blockId");
        String value=id==null?"":id.toString();return value.substring(value.lastIndexOf(':')+1);
    }
    public static void prepare(AbstractContainerMenu menu){
        var ref=SCREENS.get(menu);var screen=ref==null?null:ref.get();
        if(screen==null||!screen.getClass().getName().endsWith(".MachineScreen"))return;
        MENUS.put(menu.containerId,new WeakReference<>(menu));
        try {
            int top=(Integer)screen.getClass().getMethod("getY").invoke(screen);
            for(var child:screen.children())if(child instanceof AbstractWidget w&&w.getWidth()==20&&w.getHeight()==20&&(w.getY()-top==4||w.getY()-top==-24))w.setY(top-24);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI native control row",failure);}
    }
    public static boolean route(int syncId,Runnable nativeReceiver){
        if(ROUTING.get())return false;
        var player=Minecraft.getInstance().player;var ref=MENUS.get(syncId);var menu=ref==null?null:ref.get();
        if(player==null||menu==null||player.containerMenu==menu)return false;
        var previous=player.containerMenu;ROUTING.set(true);player.containerMenu=menu;
        try{nativeReceiver.run();}finally{player.containerMenu=previous;ROUTING.remove();}
        return true;
    }
    public static void invoke(String owner,String method,Object...arguments){
        try{
            for(var m:Class.forName(owner).getDeclaredMethods())if(m.getName().equals(method)&&m.getParameterCount()==arguments.length){m.setAccessible(true);m.invoke(null,arguments);return;}
            throw new NoSuchMethodException(owner+"."+method);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI native receiver",failure);}
    }
    public static boolean routePacket(Object packet,Object context){
        try{
            if(!(Boolean)context.getClass().getMethod("isOnClient").invoke(context))return false;
            int id=(Integer)packet.getClass().getMethod("syncId").invoke(packet);
            return route(id,()->{
                try{packet.getClass().getMethod("handle",context.getClass()).invoke(packet,context);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI native addressed update",failure);}
    }
}
