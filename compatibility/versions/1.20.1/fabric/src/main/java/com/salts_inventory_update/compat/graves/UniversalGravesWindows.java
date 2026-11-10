package com.salts_inventory_update.compat.graves;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import static com.salts_inventory_update.compat.graves.GravesAccess.*;

/** Explicit storage adapter; selection-only grave lists keep their native screen. */
public final class UniversalGravesWindows {
    public static MenuType<ChestMenu> STORAGE,LIST;
    static final ResourceLocation ACTION=new ResourceLocation("salts_inventory_compat","graves/action"),STATE=new ResourceLocation("salts_inventory_compat","graves/state");
    private static final Map<Object,Open> OPEN=new IdentityHashMap<>();
    private static boolean installed;
    private static final class Open {AbstractContainerMenu menu;boolean closed;long real=-1,take=-1;}
    private UniversalGravesWindows() { }
    public static boolean storage(Object gui){return gui!=null&&gui.getClass().getName().equals("eu.pb4.graves.ui.GraveGui");}
    private static boolean list(Object gui){return gui!=null&&Set.of("eu.pb4.graves.ui.GraveListGui","eu.pb4.graves.ui.AllGraveListGui").contains(gui.getClass().getName());}
    public static MenuType<?> typeFor(Object gui){
        if(STORAGE==null||gui==null||!(call(gui,"getPlayer") instanceof ServerPlayer p)||!DesktopSessions.shouldCapture(p))return null;
        return storage(gui)&&OPEN.containsKey(gui)?STORAGE:list(gui)?LIST:null;
    }
    public static MenuType<?> typeForMenu(AbstractContainerMenu m){return typeFor(gui(m));}
    public static synchronized void initializeCommon(){
        if(STORAGE!=null)return;
        STORAGE=Registry.register(BuiltInRegistries.MENU,new ResourceLocation("salts_inventory_compat","universal_grave"),new MenuType<>((id,inv)->new GraveClientMenu(id,inv,0,0),FeatureFlags.DEFAULT_FLAGS));
        LIST=Registry.register(BuiltInRegistries.MENU,new ResourceLocation("salts_inventory_compat","universal_grave_list"),new MenuType<>((id,inv)->new ChestMenu(LIST,id,inv,new SimpleContainer(45),5),FeatureFlags.DEFAULT_FLAGS));
        var type=type();
        DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu m){return m.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu m,Slot s){int i=m.slots.indexOf(s);return m instanceof GraveClientMenu client?i<45&&!client.real(i):s.getClass().getName().equals("eu.pb4.sgui.virtual.inventory.VirtualSlot");}
        },true,(player,m,data)->"universal-graves:"+call(field(gui(m),"grave"),"getId"),null,new DesktopMenuBindings.GesturePolicy(){
            @Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}
        });
        DesktopMenuBindings.registerStorage(type,(m,s)->m.slots.indexOf(s)<36&&!DesktopMenuBindings.isVirtualSlot(m,s));
        DesktopServerApi.registerOpeningData(type,(player,m,b)->{b.writeLong(mask(m,false,player));b.writeLong(mask(m,true,player));});
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(ServerPlayer player,AbstractContainerMenu m){return storage(gui(m));}
            @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu m){return true;}
            @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Void> c){
                var open=OPEN.get(gui(c.menu()));if(open==null)return;
                long real=mask(c.menu(),false,c.player()),take=mask(c.menu(),true,c.player());
                if(real!=open.real||take!=open.take){open.real=real;open.take=take;var b=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{b.writeLong(real);b.writeLong(take);byte[] data=new byte[16];b.readBytes(data);c.sendToClient(STATE,data);}finally{b.release();}}
            }
            @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){
                Object gui=gui(c.menu());OPEN.remove(gui);call(gui,"close",true);
                if((Boolean)call(field(gui,"grave"),"isRemoved")&&field(gui,"previousUi")!=null)call(field(gui,"previousUi"),"open");
            }
        });
        // Opt out only this non-inventory family; ordinary vanilla chests are untouched.
        DesktopServerApi.registerWindow(listType(),new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){@Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){return false;}});
        DesktopServerApi.registerPayload(type,ACTION,c->{
            byte[] data=c.data();if(data.length!=2||(data[0]&255)<36||(data[0]&255)>=45||(data[1]&255)>1){c.reject("Invalid grave action");return;}
            Object gui=gui(c.menu());var open=OPEN.get(gui);if(open==null||open.closed)return;
            var previous=c.player().containerMenu;
            var cursor=c.carriedStack();
            c.menu().setCarried(cursor.copy());
            c.player().containerMenu=c.menu();
            try{
                Class<?> clicks=Class.forName("eu.pb4.sgui.api.ClickType");Object click=clicks.getMethod("toClickType",ClickType.class,int.class,int.class).invoke(null,ClickType.PICKUP,(int)data[1],(int)data[0]);
                call(gui,"onAnyClick",(int)data[0],click,ClickType.PICKUP);call(gui,"click",(int)data[0],click,ClickType.PICKUP);
                if(c.player().containerMenu!=c.menu())open.closed=true;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native grave action",failure);}
            finally{
                if(c.player().containerMenu==c.menu())c.player().containerMenu=previous;
                // Native SGUI actions read the active menu cursor. Commit that
                // mutation through Core's canonical cursor API while the session
                // exists; a native transition can remove the temporary mirror.
                if(DesktopSessions.hasOpenSessionMatching(c.player(),m->m==c.menu()))c.setCarriedStack(c.menu().getCarried());
                else c.player().inventoryMenu.setCarried(cursor);
            }
            c.broadcastChanges();
        });
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static MenuType<AbstractContainerMenu> type(){return (MenuType)STORAGE;}
    @SuppressWarnings({"unchecked","rawtypes"}) private static MenuType<AbstractContainerMenu> listType(){return (MenuType)LIST;}
    private static Object gui(AbstractContainerMenu m){return call(m,"getGui");}
    private static long mask(AbstractContainerMenu m,boolean take,Player p){long bits=0;for(int i=0;i<36;i++){var s=m.slots.get(i);if(!s.getClass().getName().equals("eu.pb4.sgui.virtual.inventory.VirtualSlot")&&(!take||s.mayPickup(p)))bits|=1L<<i;}return bits;}
    public static Boolean isOpen(Object gui){var o=OPEN.get(gui);return o==null?null:!o.closed;}
    public static boolean valid(AbstractContainerMenu m){var o=OPEN.get(gui(m));return o==null||!o.closed&&!(Boolean)call(field(gui(m),"grave"),"isRemoved");}
    public static boolean close(Object gui){var o=OPEN.get(gui);if(o==null)return false;o.closed=true;return true;}
    public static boolean open(Object gui){
        if(!storage(gui)||STORAGE==null)return false;var player=(ServerPlayer)call(gui,"getPlayer");if(!DesktopSessions.shouldCapture(player))return false;
        try{
            // GraveGui captured its previous list before reaching sendGui.
            if(player.containerMenu!=player.inventoryMenu&&player.containerMenu.getClass().getName().startsWith("eu.pb4.sgui."))player.closeContainer();
            Class<?> slotGui=Class.forName("eu.pb4.sgui.api.gui.SlotGuiInterface");
            MenuProvider factory=(MenuProvider)Class.forName("eu.pb4.sgui.virtual.SguiScreenHandlerFactory").getMethod("ofDefault",slotGui).invoke(null,gui);
            var provider=new SimpleMenuProvider((id,inv,p)->{
                AbstractContainerMenu menu=factory.createMenu(id,inv,p);var open=new Open();open.menu=menu;OPEN.put(gui,open);
                set(gui,"screenHandler",menu);set(gui,"syncId",id);return menu;
            },title((Component)call(gui,"getTitle")));
            var result=DesktopSessions.openMenuSession(player,provider,null);
            if(result==null){OPEN.remove(gui);set(gui,"screenHandler",null);set(gui,"syncId",-1);}
            return result!=null;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native grave menu opening",failure);}
    }
    static Component title(Component title){String s=title.getString();return Component.literal(s.startsWith("-1.")?s.substring(3):s);}
    public static void initializeClient(){
        if(installed)return;installed=true;
        DesktopMenuFactories.register(STORAGE,(mc,p,id,data)->{if(data.length!=16)throw new IllegalArgumentException("Invalid grave slot roles");var b=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));try{return new GraveClientMenu(id,p.getInventory(),b.readLong(),b.readLong());}finally{b.release();}});
        GravesNativeListScreen.register(LIST);
        DesktopClientApi.registerWindow(STORAGE,new GraveWindow());
    }
}
