package com.salts_inventory_update.compat.aether;

import java.util.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuRegistration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Self-owned native equipment with authenticated Core slot and visibility actions. */
public final class AetherEquipment {
    public static MenuType<AetherEquipmentMenu> MENU;
    static final ResourceLocation VISIBILITY=AetherWindows.id("aether/visibility"),STATE=AetherWindows.id("aether/state");
    private AetherEquipment() { }
    static void initializeCommon(){
        AetherAccess.transport("initialize");
        NativeMenuRegistration.register("aether_equipment",()->new MenuType<>((i,inv)->new AetherEquipmentMenu(i,inv,new UUID(0,0)),FeatureFlags.DEFAULT_FLAGS),type->{
            MENU=type;
            DesktopMenuBindings.register(type,m->m.slots.size(),true,(p,m,data)->"aether-equipment:"+p.getUUID());
            DesktopMenuBindings.registerStorage(type,(m,s)->false);
            DesktopServerApi.registerOpeningData(type,(p,m,b)->m.writeOpening(b));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AetherEquipmentMenu,State>(){
                @Override public State createState(DesktopServerSessionContext<AetherEquipmentMenu,State> c){return new State();}
                @Override public void tick(DesktopServerSessionContext<AetherEquipmentMenu,State> c){
                    if(c.menu().topologyChanged()){DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->open(c.player(),c.menu().ownerWindow));return;}
                    byte[] next=new byte[c.menu().nativeSlots.size()];for(int i=0;i<next.length;i++)next[i]=(byte)(AetherAccess.render(c.menu().nativeSlots.get(i))?1:0);
                    if(!Arrays.equals(next,c.state().render)){c.state().render=next;c.sendToClient(STATE,next);}
                }
            });
            DesktopServerApi.registerPayload(type,VISIBILITY,c->{
                if(c.data().length!=4||c.menu().topologyChanged())return;int index=java.nio.ByteBuffer.wrap(c.data()).getInt();
                if(index<0||index>=c.menu().nativeSlots.size())return;
                AetherAccess.toggle(c.menu().nativeSlots.get(index),c.player());c.broadcastChanges();
            });
        });
    }
    public static void receive(ServerPlayer player,UUID owner){if(MENU!=null&&DesktopSessions.shouldCapture(player)&&player.isAlive()&&!player.isSpectator()&&!DesktopSessions.hasOpenSessionMatching(player,m->m instanceof AetherEquipmentMenu))open(player,owner);}
    static void open(ServerPlayer player,UUID owner){DesktopSessions.openMenuSession(player,new SimpleMenuProvider((id,inv,p)->new AetherEquipmentMenu(id,inv,owner),Component.literal("Accessories")),null);}
    private static final class State {byte[] render=new byte[0];}
}
