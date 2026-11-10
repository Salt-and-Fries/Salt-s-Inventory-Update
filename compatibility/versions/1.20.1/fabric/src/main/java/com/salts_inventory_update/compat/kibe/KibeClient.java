package com.salts_inventory_update.compat.kibe;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.kibe.KibeWindows.Kind;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** Approved Kibe layouts: original live panels, complete rims and rune items. */
public final class KibeClient {
    private static final DesktopInsets INSETS=new DesktopInsets(7,26,7,7);
    private static final DesktopChromeMetrics METRICS=new DesktopChromeMetrics(22,7,7,11,11,3,5,5,0);
    private static final Set<String> INSTALLED=new HashSet<>();
    private static final Set<State> HELD_COOLERS=Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean listening;
    private KibeClient() { }
    public static void initializeClient(){KibeWindows.clientCoolerOpen=()->!HELD_COOLERS.isEmpty();install();DesktopClientApi.onClientStarted(mc->{install();listen(mc);});}
    private static synchronized void install(){
        KibeWindows.bind();
        for(var kind:Kind.values())for(String path:kind.paths()){
            var type=NativeMenuSupport.type("kibe:"+path);
            if(type==null||!INSTALLED.add(path))continue;
            // Native transport reconstructs the original ExtendedScreenHandlerType
            // from its unchanged position, or hand + tag opening payload.
            DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","kibe/"+path),100,
                Set.of(type),lookup->true,lookup->new Template(kind,path));
        }
    }
    private static void listen(Minecraft mc){
        if(listening)return;listening=true;
        if(mc.getResourceManager() instanceof ReloadableResourceManager resources)
            resources.registerReloadListener((ResourceManagerReloadListener)manager->mc.execute(KibeChrome::reload));
    }
    static int width(Kind kind,String path,int titleWidth){
        int approved=kind==Kind.GENERATOR&&path.startsWith("cobblestone")?190:kind.width;
        return Math.max(approved,titleWidth+46);
    }
    static DesktopNativeLayout layout(Kind kind,String path,int titleWidth){
        int width=width(kind,path,titleWidth),dy=kind.runes()?18:kind.bh==18?2:0;
        var source=new DesktopWindowBounds(kind.sx,kind.sy,kind.bw,kind.bh);
        return DesktopNativeLayout.builder().canvas(width-INSETS.horizontal(),kind.height-INSETS.vertical())
            .region(new DesktopNativeRegion("native-working-panel",source,(width-INSETS.horizontal()-kind.bw)/2,dy))
            .scrollable(false).build();
    }
    static Object invoke(Object owner,String method){
        try{return owner.getClass().getMethod(method).invoke(owner);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Kibe native state: "+method,failure);}
    }
    private static void runes(DesktopRenderContext<AbstractContainerMenu,State> c,Kind kind){
        Map<?,?> colors=kind==Kind.CHEST?(Map<?,?>)invoke(invoke(c.model(),"getEntity"),"getRuneColors"):Map.of();
        CompoundTag tag=kind==Kind.BAG?(CompoundTag)invoke(c.model(),"getTag"):new CompoundTag();
        int x=c.windowX()+(c.windowWidth()-86)/2,y=c.windowY()+24;
        for(int i=1;i<=8;i++){
            DyeColor color=kind==Kind.CHEST?(DyeColor)colors.get(i):DyeColor.byName(tag.getString("rune"+i),DyeColor.WHITE);
            if(color==null)color=DyeColor.WHITE;
            c.item(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("kibe",color.getName()+"_rune"))),x+(i-1)*10,y);
        }
    }
    public static final class State {final KibeChrome chrome=new KibeChrome();}
    private record Template(Kind kind,String path)implements DesktopNativeTemplate<AbstractContainerMenu,State>{
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){if(kind==Kind.HELD_COOLER)HELD_COOLERS.add(c.state());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){return layout(kind,path,c.fontWidth(c.originalTitle()));}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowDecoration<>(){
                @Override public DesktopChromeMetrics metrics(){return METRICS;}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){draw.state().chrome.paint(draw,controls,kind.theme);}
            };
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){if(kind.runes())runes(c,kind);}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){HELD_COOLERS.remove(c.state());c.state().chrome.close();}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "kibe-approved-1:"+path;}
    }
}
