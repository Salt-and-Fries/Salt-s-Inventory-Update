package com.salts_inventory_update.compat.tradingpost;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Deliver upstream-decoded updates to their detached screen, including pre-open packets. */
public final class TradingPostPackets {
    private record Pending(Object handler,Object packet,long created) { }
    private static final Map<Integer,WeakReference<AbstractContainerScreen<?>>> SCREENS=new HashMap<>();
    private static final Map<Integer,List<Pending>> PENDING=new LinkedHashMap<>();
    private static boolean applying;
    private TradingPostPackets() { }
    static void open(AbstractContainerScreen<?> screen){
        SCREENS.put(screen.getMenu().containerId,new WeakReference<>(screen));
        var pending=PENDING.remove(screen.getMenu().containerId);
        if(pending!=null)for(var p:pending)if(System.nanoTime()-p.created()<5_000_000_000L)apply(screen,p.handler(),p.packet());
    }
    static void close(AbstractContainerMenu menu){SCREENS.remove(menu.containerId);PENDING.remove(menu.containerId);}
    public static boolean applying(){return applying;}
    public static boolean receive(Object handler,Object packet){
        if(applying)return false;
        var mc=Minecraft.getInstance();if(mc.player==null)return false;
        try {
            var field=packet.getClass().getDeclaredField("containerId");field.setAccessible(true);int id=field.getInt(packet);
            if(mc.screen instanceof AbstractContainerScreen<?> s&&s.getMenu().containerId==id
                &&s.getClass().getName().endsWith(".TradingPostScreen"))return false;
            var ref=SCREENS.get(id);var screen=ref==null?null:ref.get();
            if(screen!=null){apply(screen,handler,packet);return true;}
            long now=System.nanoTime();PENDING.values().forEach(list->list.removeIf(p->now-p.created()>5_000_000_000L));
            PENDING.values().removeIf(List::isEmpty);
            if(PENDING.size()>=16)PENDING.remove(PENDING.keySet().iterator().next());
            var list=PENDING.computeIfAbsent(id,k->new ArrayList<>());
            if(list.size()<128)list.add(new Pending(handler,packet,now));
            return false;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Trading Post native update",failure);}
    }
    private static void apply(AbstractContainerScreen<?> screen,Object handler,Object packet){
        var mc=Minecraft.getInstance();var previousScreen=mc.screen;var previousMenu=mc.player.containerMenu;
        applying=true;mc.screen=screen;mc.player.containerMenu=screen.getMenu();
        try {
            // Invoke the same decoded handler, under only its expected native
            // presentation context. No world state or packet data is invented.
            var method=handler.getClass().getDeclaredMethod("handle",packet.getClass(),Player.class,Object.class);
            method.setAccessible(true);method.invoke(handler,packet,mc.player,mc);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Trading Post update handler",failure);}
        finally{mc.player.containerMenu=previousMenu;mc.screen=previousScreen;applying=false;}
    }
}
