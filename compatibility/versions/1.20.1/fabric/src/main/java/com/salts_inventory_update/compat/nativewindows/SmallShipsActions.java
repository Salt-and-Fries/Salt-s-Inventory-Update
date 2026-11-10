package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** A native page change replaces one authenticated window instead of leaving its old page behind. */
public final class SmallShipsActions {
    private static final ResourceLocation PAGE=new ResourceLocation("salts_inventory_compat","smallships/page");
    private static boolean installed;
    private static final java.util.Set<AbstractContainerMenu> PENDING=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private SmallShipsActions() { }
    public static void initialize() {
        if(!DesktopEnvironment.isModLoaded("smallships"))return;
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("smallships",()->{
            if(installed)return;
            var nativeType=NativeMenuSupport.type("smallships:ship_container");
            var type=nativeType!=null?nativeType:NativeMenuSupport.type("minecraft:ship_container");if(type==null)return;
            DesktopServerApi.registerNativePayload(type,PAGE,c->{
                if(c.data().length!=1||!c.menu().getClass().getName().equals("com.talhanation.smallships.world.inventory.ShipContainerMenu"))return;
                int delta=c.data()[0];if(delta!=1&&delta!=-1)return;
                var owner=(Entity)ForeignMenuAccess.invoke(c.menu(),"getContainerShip");
                int page=number(c.menu(),"getPageIndex"),count=number(c.menu(),"getPageCount"),rows=number(c.menu(),"getRowCount");
                int next=Math.max(0,Math.min(page+delta,count-1));
                if(next==page||!owner.isAlive()||!c.menu().stillValid(c.player()))return;
                ForeignMenuAccess.invoke(c.menu(),"updatePaging",new Class<?>[]{int.class,int.class,int.class},rows,count,next);
                try {
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    try {
                        Class<?> entityType=Class.forName("com.talhanation.smallships.world.entity.ship.ContainerShip");
                        Class.forName("com.talhanation.smallships.world.inventory.ContainerUtility")
                            .getMethod("openShipMenu",Player.class,entityType).invoke(null,c.player(),owner);
                    }catch(ReflectiveOperationException error){throw new IllegalStateException("Unsupported native ship page action",error);}
                });
                } finally {
                    if(!DesktopSessions.hasOpenSessionMatching(c.player(),menu->menu==c.menu())) {
                        // The old page mirror was cleared by native removal.
                        // Paging consumes no items. Keep the canonical cursor
                        // through Core's enclosing native-payload callback.
                        c.menu().setCarried(c.player().inventoryMenu.getCarried().copy());
                    }
                }
            });
            installed=true;
        }));
    }
    private static int number(AbstractContainerMenu menu,String method){return ((Number)ForeignMenuAccess.invoke(menu,method)).intValue();}
    public static boolean clicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e) {
        if(e.button()!=0)return false;
        var nativeContext=(DesktopNativeContext<?,?>)c;
        var point=nativeContext.nativeSourceAt(e.x(),e.y());if(point.isEmpty())return false;
        var source=point.get();
        int bottom=nativeContext.nativeView().slots().stream().filter(s->!s.player()).mapToInt(s->s.bounds().y()+s.bounds().height()).max().orElse(Integer.MAX_VALUE);
        for(var w:nativeContext.nativeView().widgets())if(w.visible()&&w.active()&&DesktopNativeRegion.contains(w.bounds(),source.x(),source.y())) {
            // Both original buttons occupy the small pager strip below the ship's six cargo rows.
            if(w.bounds().y()<bottom||w.bounds().height()>16)continue;
            if(PENDING.contains(c.model()))return true;
            int delta=w.bounds().x()<140?-1:1;
            int page=number(c.model(),"getPageIndex"),count=number(c.model(),"getPageCount"),rows=number(c.model(),"getRowCount");
            int next=Math.max(0,Math.min(page+delta,count-1));
            if(next==page)return true;
            if(c.sendPayload(PAGE,new byte[]{(byte)delta})) {
                // The original native button makes this same local prediction before its opening packet.
                ForeignMenuAccess.invoke(c.model(),"updatePaging",new Class<?>[]{int.class,int.class,int.class},rows,count,next);PENDING.add(c.model());
            }
            return true;
        }
        return false;
    }
}
