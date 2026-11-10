package com.salts_inventory_update.compat.aether;

import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Original Aether menus and complete working artwork, with the integrated inventory removed. */
public final class AetherWindows {
    enum Kind {
        ALTAR("altar","AltarMenu",158,92,box(18,15,125,56)),
        FREEZER("freezer","FreezerMenu",158,92,box(18,15,125,56)),
        INCUBATOR("incubator","IncubatorMenu",146,92,box(35,15,78,56)),
        LORE("book_of_lore","LoreBookMenu",256,168,box(12,2,234,105));
        final String path,menu;final int width,height;final DesktopWindowBounds body;
        Kind(String path,String menu,int width,int height,DesktopWindowBounds body){this.path=path;this.menu="com.aetherteam.aether.inventory.menu."+menu;this.width=width;this.height=height;this.body=body;}
    }
    private static final Set<Kind> BOUND=new HashSet<>(),CLIENT=new HashSet<>();
    private static final DesktopInsets INSETS=new DesktopInsets(7,26,7,8);
    private AetherWindows() { }
    static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("salts_inventory_compat",path);}
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());AetherEquipment.initializeCommon();}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});AetherEquipmentClient.initialize();}
    private static synchronized void bind(){
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("aether:"+kind.path);if(type==null||!BOUND.add(kind))continue;
            NativeMenuSupport.support(type,kind.menu,true);
            // A registered detached factory requires explicit opening data,
            // even though these native constructors need no block argument.
            DesktopServerApi.registerOpeningData(type,(player,menu,data)->data.writeByte(1));
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
        }
    }
    private static synchronized void install(){
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("aether:"+kind.path);if(type==null||!CLIENT.add(kind))continue;
            DesktopMenuFactories.register(type,(mc,player,id,data)->{
                if(data.length!=1||data[0]!=1)throw new IllegalArgumentException("Unexpected Aether opening data");
                return AetherAccess.menu(kind.menu,id,player.getInventory());
            });
            DesktopClientApi.registerNativeMatcher(id("aether/"+kind.path),100,Set.of(type),lookup->true,lookup->new Template(kind));
        }
    }
    static DesktopNativeLayout layout(Kind kind,int titleWidth){
        int width=Math.max(kind.width,titleWidth+46)-INSETS.horizontal();
        int x=(width-kind.body.width())/2;
        var b=DesktopNativeLayout.builder().canvas(width,kind.height-INSETS.vertical()).scrollable(false)
            .keep("aether-"+kind.path,kind.body,x,1);
        if(kind==Kind.LORE){
            b.keep("previous-page",box(14,169,20,20),2,113)
                .keep("previous-label",box(12,155,30,10),27,118)
                .keep("next-page",box(221,169,20,20),width-22,113)
                .keep("next-label",box(219,155,27,10),width-55,118);
        }
        return b.build();
    }
    private record Template(Kind kind) implements DesktopNativeTemplate<AbstractContainerMenu,Void>{
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){return layout(kind,c.fontWidth(c.originalTitle()));}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "aether-reviewed-1:"+kind.path;}
    }
    private static DesktopWindowBounds box(int x,int y,int width,int height){return new DesktopWindowBounds(x,y,width,height);}
}
