package com.salts_inventory_update.compat.tradingpost;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Deliver upstream-decoded updates to their detached screen, including pre-open packets. */
public final class TradingPostPackets {
    private record Pending(Object handler,Object packet,Object context,long created) { }
    private static final Map<Integer,WeakReference<AbstractContainerScreen<?>>> SCREENS=new HashMap<>();
    private static final Map<Integer,List<Pending>> PENDING=new LinkedHashMap<>();
    private static boolean applying;
    private TradingPostPackets() { }
    static void open(AbstractContainerScreen<?> screen){
        SCREENS.put(screen.getMenu().containerId,new WeakReference<>(screen));
        var pending=PENDING.remove(screen.getMenu().containerId);
        if(pending!=null)for(var p:pending)if(System.nanoTime()-p.created()<5_000_000_000L)apply(screen,p.handler(),p.packet(),p.context());
    }
    static void close(AbstractContainerMenu menu){SCREENS.remove(menu.containerId);PENDING.remove(menu.containerId);}
    public static boolean applying(){return applying;}
    public static boolean receiveV4(Object handler,Object context){
        try{
            var outer=handler.getClass().getDeclaredField("this$0");outer.setAccessible(true);
            return receive(handler,outer.get(handler),context);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Trading Post listener owner",error);}
    }
    private static boolean receive(Object handler,Object packet,Object context){
        if(applying)return false;
        var mc=Minecraft.getInstance();if(mc.player==null)return false;
        try {
            var field=packet.getClass().getDeclaredField("containerId");field.setAccessible(true);int id=field.getInt(packet);
            if(mc.gui.screen() instanceof AbstractContainerScreen<?> s&&s.getMenu().containerId==id
                &&s.getClass().getName().endsWith(".TradingPostScreen"))return false;
            var ref=SCREENS.get(id);var screen=ref==null?null:ref.get();
            if(screen!=null){apply(screen,handler,packet,context);return true;}
            long now=System.nanoTime();PENDING.values().forEach(list->list.removeIf(p->now-p.created()>5_000_000_000L));
            PENDING.values().removeIf(List::isEmpty);
            if(PENDING.size()>=16)PENDING.remove(PENDING.keySet().iterator().next());
            var list=PENDING.computeIfAbsent(id,k->new ArrayList<>());
            if(list.size()<128)list.add(new Pending(handler,packet,context,now));
            return false;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Trading Post native update",failure);}
    }
    private static void apply(AbstractContainerScreen<?> screen,Object handler,Object packet,Object context){
        var mc=Minecraft.getInstance();var previousScreen=mc.gui.screen();var previousMenu=mc.player.containerMenu;
        applying=true;((com.salts_inventory_update.compat.nativewindows.mixin.NativeGuiScreenAccessor)mc.gui).salts_inventory_compat$nativeScreen(screen);mc.player.containerMenu=screen.getMenu();
        try {
            // Invoke the same decoded handler, under only its expected native
            // presentation context. No world state or packet data is invented.
            // Invoke the publisher's bridge with its original decoded context.
            // The temporary menu belongs only to this authenticated desktop session.
            var method=handler.getClass().getDeclaredMethod("accept",Object.class);
            method.setAccessible(true);method.invoke(handler,context);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Trading Post update handler",failure);}
        finally{mc.player.containerMenu=previousMenu;((com.salts_inventory_update.compat.nativewindows.mixin.NativeGuiScreenAccessor)mc.gui).salts_inventory_compat$nativeScreen(previousScreen);applying=false;}
    }
}
