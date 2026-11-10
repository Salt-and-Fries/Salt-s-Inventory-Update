package com.salts_inventory_update.compat.quark;

import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.Minecraft;

/** One lifetime and one movement group, while retaining each inventory's focus/sort actions. */
public final class QuarkBackpackLink {
    private static final Map<UUID,Pair> PAIRS=new HashMap<>();
    private static final Map<UUID,Long> PENDING=new HashMap<>();
    private static final class Pair {
        final QuarkBackpackMenu menu;final DesktopWindowHandle bag;
        DesktopWindowState parentState,bagState;boolean positioned;
        Pair(QuarkBackpackMenu menu,DesktopWindowHandle bag) { this.menu=menu;this.bag=bag; }
    }
    private QuarkBackpackLink() { }
    public static boolean paired(UUID window) { return PAIRS.containsKey(window)||PAIRS.values().stream().anyMatch(p->p.bag.instanceId().equals(window)); }
    static void opened(QuarkBackpackMenu menu,DesktopWindowHandle bag) { PENDING.remove(menu.ownerWindow);PAIRS.put(menu.ownerWindow,new Pair(menu,bag)); }
    static void closed(QuarkBackpackMenu menu,DesktopWindowHandle bag) { var pair=PAIRS.get(menu.ownerWindow);if(pair!=null&&pair.bag.instanceId().equals(bag.instanceId()))PAIRS.remove(menu.ownerWindow); }
    private static void dispose(Pair pair) {
        pair.menu.internalClosing=true;try { pair.bag.close(); }finally { pair.menu.internalClosing=false; }
    }
    public static void tick(Minecraft mc) {
        if(mc.player==null) { PAIRS.clear();PENDING.clear();return; }
        var workspace=DesktopClientApi.currentWorkspace().orElse(null);
        var windows=workspace==null?List.<DesktopWorkspaceWindow>of():workspace.windows();
        for(var entry:new ArrayList<>(PAIRS.entrySet())) {
            var pair=entry.getValue();var parent=windows.stream().filter(w->w.handle().instanceId().equals(entry.getKey())).map(DesktopWorkspaceWindow::handle).findFirst().orElse(null);
            if(!pair.bag.isOpen()) { PAIRS.remove(entry.getKey());continue; }
            if(parent==null||!parent.isOpen()||!parent.state().visible()||!QuarkAccess.wearing(mc.player)||!DesktopClientApi.isDesktopAvailable()) { dispose(pair);continue; }
            var a=parent.state();var b=pair.bag.state();
            if(!pair.positioned) {
                set(pair.bag,DesktopWindowAction.UNLOCK);set(pair.bag,DesktopWindowAction.UNPIN);
                int y=Math.max(0,a.y()-b.height()-4),x=Math.max(0,a.x()+(a.width()-b.width())/2);
                pair.bag.moveTo(x,y);
                if(a.y()<b.height()+4)parent.moveTo(a.x(),y+b.height()+4);
                pin(pair.bag,a.pinned());lock(pair.bag,a.locked());pair.positioned=true;
            } else {
                boolean parentPin=a.pinned()!=pair.parentState.pinned(),bagPin=b.pinned()!=pair.bagState.pinned();
                boolean parentLock=a.locked()!=pair.parentState.locked(),bagLock=b.locked()!=pair.bagState.locked();
                if(parentPin)pin(pair.bag,a.pinned());else if(bagPin)pin(parent,b.pinned());
                if(parentLock)lock(pair.bag,a.locked());else if(bagLock)lock(parent,b.locked());
                int adx=a.x()-pair.parentState.x(),ady=a.y()-pair.parentState.y(),bdx=b.x()-pair.bagState.x(),bdy=b.y()-pair.bagState.y();
                int dx=adx!=0||ady!=0?adx:bdx,dy=adx!=0||ady!=0?ady:bdy;
                int ax=pair.parentState.x()+dx,ay=pair.parentState.y()+dy,bx=pair.bagState.x()+dx,by=pair.bagState.y()+dy;
                // Fit the intended positions together, before either handle's individual clamp.
                int vw=mc.getWindow().getGuiScaledWidth(),vh=mc.getWindow().getGuiScaledHeight();
                int fitX=Math.max(0,-Math.min(ax,bx))-Math.max(0,Math.max(ax+a.width(),bx+b.width())-vw);
                int fitY=Math.max(0,-Math.min(ay,by))-Math.max(0,Math.max(ay+a.height(),by+b.height())-vh);
                if(dx!=0||dy!=0||fitX!=0||fitY!=0) { moveFollower(parent,ax+fitX,ay+fitY);moveFollower(pair.bag,bx+fitX,by+fitY); }
            }
            pair.parentState=parent.state();pair.bagState=pair.bag.state();
        }
        if(workspace==null||!workspace.isNativeScreenActive()||!QuarkClient.useInventoryPair()||!QuarkAccess.wearing(mc.player))return;
        for(var w:windows)if(w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY&&w.handle().isOpen()&&w.handle().state().visible()&&!PAIRS.containsKey(w.handle().instanceId())) {
            UUID owner=w.handle().instanceId();long now=System.nanoTime();if(now-PENDING.getOrDefault(owner,0L)<2_000_000_000L)continue;PENDING.put(owner,now);
            try { Class.forName("com.salts_inventory_update.compat.quark.neoforge.QuarkNeoForgeTransport").getMethod("open",UUID.class).invoke(null,owner); }
            catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
    }
    private static void set(DesktopWindowHandle h,DesktopWindowAction action) { if(h.available(action))h.request(action); }
    private static void pin(DesktopWindowHandle h,boolean value) { set(h,value?DesktopWindowAction.PIN:DesktopWindowAction.UNPIN); }
    private static void lock(DesktopWindowHandle h,boolean value) { set(h,value?DesktopWindowAction.LOCK:DesktopWindowAction.UNLOCK); }
    private static void moveFollower(DesktopWindowHandle h,int x,int y) {
        boolean locked=h.state().locked();if(locked)lock(h,false);h.moveTo(x,y);if(locked)lock(h,true);
    }
}
