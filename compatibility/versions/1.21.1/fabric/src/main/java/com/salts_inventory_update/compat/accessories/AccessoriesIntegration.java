package com.salts_inventory_update.compat.accessories;

import java.util.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import static com.salts_inventory_update.compat.accessories.AccessoriesAccess.*;

/** Self-owned Accessories only; entity editing keeps its original native screen. */
public final class AccessoriesIntegration {
    public static MenuType<AccessoriesDesktopMenu> MENU;
    static final ResourceLocation VISIBILITY=id("accessories/visibility"),STATE=id("accessories/state");
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("salts_inventory_compat",path);}
    private AccessoriesIntegration() { }
    public static void initializeCommon(){
        if(!AccessoriesTransport.supported())return;AccessoriesTransport.initialize();
        com.salts_inventory_update.compat.nativewindows.NativeMenuRegistration.register("accessories_equipment",()->new MenuType<>((i,inv)->new AccessoriesDesktopMenu(i,inv,new UUID(0,0)),FeatureFlags.DEFAULT_FLAGS),AccessoriesIntegration::bindMenu);
    }
    private static void bindMenu(MenuType<AccessoriesDesktopMenu> type){
        MENU=type;
        DesktopMenuBindings.register(MENU,m->m.slots.size(),true,(p,m,d)->"accessories:"+p.getUUID(),null,null);
        DesktopMenuBindings.registerStorage(MENU,(m,s)->false);
        DesktopServerApi.registerOpeningData(MENU,(p,m,b)->m.writeOpening(b));
        DesktopServerApi.registerWindow(MENU,new DesktopServerWindowHandler<AccessoriesDesktopMenu,State>(){
            @Override public State createState(DesktopServerSessionContext<AccessoriesDesktopMenu,State> c){return new State();}
            @Override public void tick(DesktopServerSessionContext<AccessoriesDesktopMenu,State> c){
                if(c.menu().topologyChanged()){
                    DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->open(c.player(),c.menu().ownerWindow));return;
                }
                byte[] state=renderState(c.menu());if(!Arrays.equals(state,c.state().render)){c.state().render=state;c.sendToClient(STATE,state);}
            }
        });
        DesktopServerApi.registerPayload(MENU,VISIBILITY,c->{
            byte[] data=c.data();if(data.length!=4||c.menu().topologyChanged())return;int index=java.nio.ByteBuffer.wrap(data).getInt();
            if(index<0||index>=c.menu().cells.size())return;var cell=c.menu().cells.get(index);if(cell.cosmetic)return;
            try{Object packet=type("networking.server.SyncCosmeticToggle").getConstructor(Integer.class,String.class,int.class).newInstance(null,cell.group.name(),cell.index);call(type("networking.server.SyncCosmeticToggle"),"handlePacket",packet,c.player());}
            catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}c.broadcastChanges();
        });
        // Native entity menus have ownership/permission semantics outside this self-equipment panel.
        @SuppressWarnings("unchecked") var nativeType=(MenuType<AbstractContainerMenu>)BuiltInRegistries.MENU.get(ResourceLocation.fromNamespaceAndPath("accessories","accessories_menu"));
        if(nativeType!=null)DesktopServerApi.registerWindow(nativeType,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){@Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){return false;}});
    }
    static byte[] renderState(AccessoriesDesktopMenu menu){byte[] data=new byte[menu.cells.size()];for(int i=0;i<data.length;i++){var cell=menu.cells.get(i);data[i]=(byte)((Boolean)call(cell.backing,"shouldRender",cell.index)?1:0);}return data;}
    public static void open(ServerPlayer p,UUID owner){if(MENU!=null&&DesktopSessions.shouldCapture(p)&&p.isAlive()&&!p.isSpectator())DesktopSessions.openMenuSession(p,new SimpleMenuProvider((id,inv,ignored)->new AccessoriesDesktopMenu(id,inv,owner),Component.literal("Accessories")),null);}
    public static void initializeClient(){if(MENU!=null)AccessoriesClient.initialize();}
    static final class State {byte[] render=new byte[0];}
}
