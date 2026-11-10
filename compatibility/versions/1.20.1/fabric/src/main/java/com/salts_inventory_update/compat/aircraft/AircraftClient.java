package com.salts_inventory_update.compat.aircraft;

import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import static com.salts_inventory_update.compat.aircraft.AircraftDesktopMenu.Zone;

/** The cargo windows have real handles and sessions; closing the central window closes its wings. */
final class AircraftClient {
    private static final Map<UUID,Dock> DOCKS=new HashMap<>();
    private static final class Dock {
        final EnumMap<Zone,DesktopWindowHandle> windows=new EnumMap<>(Zone.class);
        final EnumMap<Zone,AircraftDesktopMenu> models=new EnumMap<>(Zone.class);
        int x=Integer.MIN_VALUE,y,width,height,viewportWidth,viewportHeight;boolean placed,dirty=true,closing;
    }
    static void initialize(){
        DesktopMenuFactories.register(AircraftWindows.MENU,(mc,p,id,data)->{
            var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try{
                int entity=b.readInt(),zone=b.readUnsignedByte();if(zone>=Zone.values().length||b.isReadable()||mc.level==null)throw new IllegalArgumentException("Invalid Aircraft opening data");
                var vehicle=mc.level.getEntity(entity);if(vehicle==null||!vehicle.getClass().getName().startsWith("immersive_aircraft."))throw new IllegalArgumentException("Missing Aircraft owner");
                return new AircraftDesktopMenu(id,p.getInventory(),vehicle,Zone.values()[zone]);
            }finally{b.release();}
        });
        DesktopClientApi.registerWindow(AircraftWindows.MENU,new AircraftPanel());
    }
    static void opened(DesktopWindowContext<AircraftDesktopMenu,AircraftPanel.State> c){var dock=DOCKS.computeIfAbsent(c.model().vehicle.getUUID(),k->new Dock());dock.windows.put(c.model().zone,c.windowHandle());dock.models.put(c.model().zone,c.model());dock.dirty=true;}
    static void tick(DesktopWindowContext<AircraftDesktopMenu,AircraftPanel.State> c){
        var dock=DOCKS.get(c.model().vehicle.getUUID());if(dock==null)return;var main=dock.windows.get(Zone.MAIN);
        if(c.model().zone!=Zone.MAIN){if((main==null||!main.isOpen())&&++c.state().orphanTicks>20)c.windowHandle().close();return;}
        if(main==null||!main.isOpen())return;var m=main.state();var env=c.environment();
        if(!dock.dirty&&dock.x==m.x()&&dock.y==m.y()&&dock.width==m.width()&&dock.height==m.height()&&dock.viewportWidth==env.viewportWidth()&&dock.viewportHeight==env.viewportHeight())return;
        var port=dock.windows.get(Zone.PORT);var star=dock.windows.get(Zone.STARBOARD);int left=port!=null&&port.isOpen()?port.state().width()-1:0,right=star!=null&&star.isOpen()?star.state().width()-1:0;
        int x=m.x(),y=m.y();
        int tallest=Math.max(m.height(),Math.max(port==null?0:port.state().height(),star==null?0:star.state().height()));
        if(!dock.placed){x=Math.max(left+4,(env.viewportWidth()-m.width()-left-right)/2+left);y=Math.max(8,(env.viewportHeight()-tallest)/2-35);dock.placed=true;}
        x=Math.max(left+4,Math.min(Math.max(left+4,env.viewportWidth()-m.width()-right-4),x));
        y=Math.max(4,Math.min(Math.max(4,env.viewportHeight()-tallest-4),y));
        boolean placed=move(dock,Zone.MAIN,x,y);m=main.state();if(port!=null&&port.isOpen())placed&=move(dock,Zone.PORT,m.x()-port.state().width()+1,m.y());if(star!=null&&star.isOpen())placed&=move(dock,Zone.STARBOARD,m.x()+m.width()-1,m.y());
        dock.x=m.x();dock.y=m.y();dock.width=m.width();dock.height=m.height();dock.viewportWidth=env.viewportWidth();dock.viewportHeight=env.viewportHeight();dock.dirty=!placed;
    }
    private static boolean move(Dock dock,Zone zone,int x,int y){
        var h=dock.windows.get(zone);var s=h.state();if(s.x()==x&&s.y()==y)return true;
        // Only this synchronous parent-layout operation can move a cargo wing.
        // Mouse/controller/handle movement sees movable=false outside this scope.
        var model=dock.models.get(zone);boolean previous=model.followingParent;model.followingParent=true;
        try{
            // Salt marks immovable panes locked. Unlocking does not reset their
            // geometry, and this permission exists only during parent placement.
            if(h.state().locked()&&!h.request(DesktopWindowAction.UNLOCK))return false;
            return h.moveTo(x,y);
        }finally{model.followingParent=previous;}
    }
    static void closed(DesktopWindowContext<AircraftDesktopMenu,AircraftPanel.State> c){
        UUID key=c.model().vehicle.getUUID();var dock=DOCKS.get(key);if(dock==null)return;var stored=dock.windows.get(c.model().zone);
        // Handles are fresh facades per callback; their stable identity is the instance UUID.
        if(stored==null||!stored.instanceId().equals(c.windowHandle().instanceId()))return;
        dock.windows.remove(c.model().zone);
        dock.models.remove(c.model().zone);
        if(c.model().zone==Zone.MAIN&&!dock.closing){dock.closing=true;for(var handle:List.copyOf(dock.windows.values()))if(handle.isOpen())handle.close();}
        if(dock.windows.isEmpty())DOCKS.remove(key);
    }
}
