package com.salts_inventory_update.compat.prominent;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** The native shop, portrait and tabs under the same stone header as the forge. */
public final class ProminentShopWindows {
    public static final String MENU="elocindev.prominentbase.client.gui.CinderTraderScreenHandler";
    private static final String SCREEN="elocindev.prominentbase.client.gui.CinderTradeScreen";
    private static final ResourceLocation SYNC=new ResourceLocation("salts_inventory_compat","prominent/shop_properties");
    private static final ResourceLocation ACTION=new ResourceLocation("owo","local_packet");
    private static final ResourceLocation ART=new ResourceLocation("prominent","textures/gui/primordial_forge.png");
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final int WIDTH=242,HEIGHT=141;
    private static final Map<AbstractContainerMenu,Long> SIZES=new WeakHashMap<>();
    private static boolean bound,client;
    private ProminentShopWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type("prominent:cinder_trader");if(type==null||bound)return;
        NativeMenuSupport.bindNative(type,false);DesktopMenuBindings.registerStorage(type,(m,s)->false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,SyncState>(){
            @Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){if(!m.getClass().getName().equals(MENU))return false;ProminentAccess.attach(m,p);return m.stillValid(p);}
            @Override public SyncState createState(DesktopServerSessionContext<AbstractContainerMenu,SyncState> c){return new SyncState();}
            @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,SyncState> c){sync(c);}
            @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,SyncState> c){sync(c);}
        });
        DesktopServerApi.registerNativePayload(type,ACTION,c->{
            if(c.data().length>32){c.reject("Oversized shop action");return;}
            var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            try{
                if(b.readVarInt()!=0)throw new IllegalArgumentException();
                Class<?> record=Class.forName(MENU+"$Trade");
                Object serializer=Class.forName("io.wispforest.owo.network.serialization.PacketBufSerializer").getMethod("get",Class.class).invoke(null,record);
                @SuppressWarnings("unchecked") Object action=((Function<FriendlyByteBuf,?>)ForeignMenuAccess.invoke(serializer,"deserializer")).apply(b);
                int index=(int)ForeignMenuAccess.invoke(action,"tradeIndex");
                var trades=(List<?>)ForeignMenuAccess.invoke(ForeignMenuAccess.invoke(ProminentAccess.field(c.menu(),"trades"),"get"),"trades");
                if(b.isReadable()||index<0||index>=trades.size())throw new IllegalArgumentException();
            }catch(ReflectiveOperationException|RuntimeException bad){c.reject("Invalid shop action");return;}
            finally{b.release();}
            DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{ForeignMenuAccess.invoke(c.menu(),"owo$handlePacket",new Class<?>[]{FriendlyByteBuf.class,boolean.class},data,false);}
                finally{data.release();}
            });
            c.broadcastChanges();
        });
        bound=true;
    }
    /** Owo's unaddressed property packet must not target a different active menu. */
    public static boolean ownsProperties(AbstractContainerMenu menu){
        if(!menu.getClass().getName().equals(MENU))return false;
        Object player=ForeignMenuAccess.invoke(menu,"player");
        return player instanceof ServerPlayer p&&DesktopSessions.hasOpenSessionMatching(p,m->m==menu);
    }
    private static final class SyncState { boolean sent; }
    private static void sync(DesktopServerSessionContext<AbstractContainerMenu,SyncState> c){
        Object offers=ProminentAccess.field(c.menu(),"trades"),portrait=ProminentAccess.field(c.menu(),"serkonidId");
        if(c.state().sent&&!(boolean)ForeignMenuAccess.invoke(offers,"needsSync")&&!(boolean)ForeignMenuAccess.invoke(portrait,"needsSync"))return;
        var b=new FriendlyByteBuf(Unpooled.buffer());
        try{
            Object list=ForeignMenuAccess.invoke(offers,"get");
            b.writeBoolean(((List<?>)ForeignMenuAccess.invoke(list,"trades")).isEmpty());
            b.writeVarInt(2);
            for(Object property:List.of(portrait,offers)){
                b.writeVarInt((int)ForeignMenuAccess.invoke(property,"index"));
                ForeignMenuAccess.invoke(property,"write",new Class<?>[]{FriendlyByteBuf.class},b);
            }
            byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);c.sendToClient(SYNC,bytes);c.state().sent=true;
        }finally{b.release();}
    }
    public static void initializeClient(){DesktopClientApi.onClientStarted(mc->{
        bind();var type=NativeMenuSupport.type("prominent:cinder_trader");if(type==null||client)return;
        DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
            try{
                ProminentAccess.attach(menu,inventory.player);
                var screen=(AbstractContainerScreen<?>)Class.forName(SCREEN).getConstructor(menu.getClass(),Inventory.class,Component.class).newInstance(menu,inventory,title);
                ProminentAccess.remember(menu,screen);return screen;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native shop screen",failure);}
        });
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","prominent/shop"),100,Set.of(type),lookup->true,lookup->new Template());client=true;
    });}
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c){return Component.literal("Cinder Shop");}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,Void> c){
            var screen=ProminentAccess.screen(c.model());Object root=ProminentAccess.field(ProminentAccess.field(screen,"uiAdapter"),"rootComponent");if(root==null)return;
            try{
                Class<?> surface=Class.forName("io.wispforest.owo.ui.core.Surface");
                ForeignMenuAccess.invoke(root,"surface",new Class<?>[]{surface},surface.getField("BLANK").get(null));
                long size=((long)screen.width<<32)|Integer.toUnsignedLong(screen.height);Long before=SIZES.put(c.model(),size);
                if(before!=null&&before!=size){var f=Class.forName(SCREEN).getDeclaredField("forReset");f.setAccessible(true);f.setBoolean(screen,true);}
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native shop surface",failure);}
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){return DesktopNativeLayout.builder().keep("original-shop",new DesktopWindowBounds(-64,-28,WIDTH,HEIGHT),0,0).canvas(WIDTH,HEIGHT).scrollable(false).build();}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,18,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowDecoration<>(){
            @Override public DesktopChromeMetrics metrics(){return new DesktopChromeMetrics(18,6,5,11,11,3,8,3,0);}
            @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> draw,List<DesktopControlState> controls){paint(draw,controls);}
        };}
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,Void> c,ResourceLocation channel,byte[] bytes){
            if(!SYNC.equals(channel))return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try{
                // Native closes when its last available group disappears. Keep
                // that close scoped to this window, rather than the desktop.
                if(b.readBoolean()){c.windowHandle().close();return;}
                ForeignMenuAccess.invoke(c.model(),"owo$readPropertySync",new Class<?>[]{FriendlyByteBuf.class},b);
                if(b.isReadable())throw new IllegalArgumentException("Trailing shop properties");
            }finally{b.release();}
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){SIZES.remove(c.model());ProminentAccess.forget(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "prominent-original-shop-1";}
    }
    private static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls){
        int x=c.windowX(),y=c.windowY();
        c.texture(ART,x,y,0,0,8,18,8,18,512,512);
        c.texture(ART,x+8,y,8,0,WIDTH-16,18,162,18,512,512);
        c.texture(ART,x+WIDTH-8,y,170,0,8,18,8,18,512,512);
        Component title=Component.literal("Cinder Shop");Object screen=ProminentAccess.screen((AbstractContainerMenu)c.model());
        Object label=ProminentAccess.field(screen,"nextChangeLabel");
        if(label!=null&&(long)ProminentAccess.field(screen,"remainingTime")>0)title=(Component)ForeignMenuAccess.invoke(label,"text");
        c.text(c.trimToWidth(title.getString(),148),x+6,y+5,0xffeeeeee,true);
        for(var s:controls){
            int column=switch(s.control()){case FOCUS->0;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;default->-1;};
            if(column<0)continue;var b=s.bounds();c.texture(CONTROLS,b.x(),b.y(),column*11,(s.pressed()||s.selected()?2:s.hovered()||s.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
