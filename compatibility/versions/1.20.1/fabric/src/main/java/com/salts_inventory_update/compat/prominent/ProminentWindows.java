package com.salts_inventory_update.compat.prominent;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Original forge surfaces; the trader is deliberately outside this adapter. */
public final class ProminentWindows {
    enum Kind {
        UPGRADE("primordial_forge_upgrade","PrimordialForgeUpgradeScreen",178,new DesktopWindowBounds(-1,-11,178,117)),
        ARMOR("primordial_forge","PrimordialForgeScreen",248,new DesktopWindowBounds(-36,-45,248,163));
        final String id,screen,menu;final int width;final DesktopWindowBounds body;
        Kind(String id,String screen,int width,DesktopWindowBounds body){this.id="prominent:"+id;this.screen="elocindev.prominentbase.client.gui."+screen;this.menu=this.screen+"Handler";this.width=width;this.body=body;}
    }
    private static final ResourceLocation ART=new ResourceLocation("prominent","textures/gui/primordial_forge.png");
    private static final ResourceLocation CINDERS=new ResourceLocation("prominent","textures/gui/cinders.png");
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final Set<Kind> BOUND=EnumSet.noneOf(Kind.class),CLIENT=EnumSet.noneOf(Kind.class);
    private ProminentWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    private static synchronized void bind(){for(var kind:Kind.values()){
        var type=NativeMenuSupport.type(kind.id);if(type==null||!BOUND.add(kind))continue;
        NativeMenuSupport.bindNative(type,false);
        DesktopMenuBindings.registerStorage(type,(m,s)->false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){
                if(!menu.getClass().getName().equals(kind.menu))return false;
                ProminentAccess.attach(menu,player);return menu.stillValid(player);
            }
        });
        ProminentPackets.register(type,kind);
    }}
    public static void initializeClient(){DesktopClientApi.onClientStarted(mc->{bind();for(var kind:Kind.values()){
        var type=NativeMenuSupport.type(kind.id);if(type==null||!CLIENT.add(kind))continue;
        DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
            try{
                ProminentAccess.attach(menu,inventory.player);
                var screen=(AbstractContainerScreen<?>)Class.forName(kind.screen).getConstructor(menu.getClass(),Inventory.class,Component.class).newInstance(menu,inventory,title);
                ProminentAccess.remember(menu,screen);return screen;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native forge screen",failure);}
        });
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","prominent/"+kind.name().toLowerCase(Locale.ROOT)),100,Set.of(type),lookup->true,lookup->new Template(kind));
    }});}
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        private final Kind kind;Template(Kind kind){this.kind=kind;}
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c){return Component.literal(kind==Kind.UPGRADE?"Primordial Forge":"Armor Selection");}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,Void> c){ProminentAccess.prepare(c.model());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){return DesktopNativeLayout.builder().keep("native-forge",kind.body,0,0).canvas(kind.width,kind.body.height()).scrollable(false).build();}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,18,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowDecoration<>(){
            @Override public DesktopChromeMetrics metrics(){return new DesktopChromeMetrics(18,6,5,11,11,3,8,3,0);}
            @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> draw,List<DesktopControlState> controls){paint(draw,controls,kind.width);}
        };}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){ProminentAccess.forget(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "prominent-original-forge-header-1:"+kind;}
    }
    private static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,int width){
        int x=c.windowX(),y=c.windowY();
        if(width==178)c.texture(ART,x,y,0,0,178,18,178,18,512,512);
        else{
            // Preserve both curved ends; extend only the center of the same strip.
            c.texture(ART,x,y,0,0,8,18,8,18,512,512);
            c.texture(ART,x+8,y,8,0,width-16,18,162,18,512,512);
            c.texture(ART,x+width-8,y,170,0,8,18,8,18,512,512);
        }
        // Preserve the native balance formerly anchored to the screen corner.
        c.texture(CINDERS,x+5,y+1,0,0,16,16,16,16,16,16);
        c.text(Integer.toString(ProminentAccess.cinders(c.minecraft().player)),x+25,y+5,0xffeeeeee,true);
        for(var s:controls){
            int column=switch(s.control()){case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;};
            if(column<0)continue;var b=s.bounds();
            c.texture(CONTROLS,b.x(),b.y(),column*11,(s.pressed()||s.selected()?2:s.hovered()||s.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
