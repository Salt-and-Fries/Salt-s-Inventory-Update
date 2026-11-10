package com.salts_inventory_update.compat.powah;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.powah.PowahWindows.Kind;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Approved working panels, native ghost icons and complete attached controls. */
public final class PowahClient {
    private static final DesktopInsets INSETS=new DesktopInsets(7,25,7,7);
    private static final DesktopChromeMetrics METRICS=new DesktopChromeMetrics(22,7,7,11,11,3,5,5,0);
    private static final Set<Kind> INSTALLED=EnumSet.noneOf(Kind.class);
    private static final Map<String,List<DesktopWindowBounds>> MASKS=new HashMap<>();
    private static boolean listening;
    private static final Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,State>> CONTEXTS=new IdentityHashMap<>();
    /** Positional publisher actions can only originate from their matching live
     * native working panel. No unrelated full-screen UI is intercepted. */
    public static boolean send(Object message) {
        int id=PowahPackets.index(message);
        if(id<0||id==4||DesktopClientApi.currentWorkspace().isEmpty())return false;
        var pos=(net.minecraft.core.BlockPos)PowahWindows.invoke(message,"pos");
        for(var entry:CONTEXTS.entrySet())if(entry.getValue().windowHandle().isOpen()&&PowahWindows.tile(entry.getKey()).getBlockPos().equals(pos)) {
            var c=entry.getValue();c.sendPayload(PowahWindows.ACTION,PowahPackets.encode(message,c.minecraft().player));return true;
        }
        return false;
    }
    public static boolean sendTank(Object menu,boolean drain) {
        var c=CONTEXTS.get(menu);
        if(c==null||!c.windowHandle().isOpen()||DesktopClientApi.currentWorkspace().isEmpty())return false;
        try {
            Object message=Class.forName(PowahPackets.PREFIX+"InteractWithTankPacket").getConstructor(int.class,boolean.class)
                .newInstance(((AbstractContainerMenu)menu).containerId,drain);
            c.sendPayload(PowahWindows.ACTION,PowahPackets.encode(message,c.minecraft().player));return true;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah tank action",failure);}
    }
    private PowahClient() { }
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{install();listen(mc);});}

    private static synchronized void install() {
        PowahWindows.bind();
        for(var kind:Kind.values()) {
            var type=NativeMenuSupport.type("powah:"+kind.path);
            if(type==null||!INSTALLED.add(kind))continue;
            DesktopMenuFactories.register(type,(mc,player,id,data)->PowahWindows.create(kind,id,player.getInventory(),data));
            DesktopClientApi.registerNativeMatcher(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","powah/"+kind.path),100,
                Set.of(type),lookup->true,lookup->new Template(kind));
        }
    }
    private static void listen(Minecraft mc) {
        if(listening)return;listening=true;
        if(mc.getResourceManager() instanceof ReloadableResourceManager resources)
            resources.registerReloadListener((ResourceManagerReloadListener)manager->mc.execute(()->{MASKS.clear();PowahChrome.reload();}));
    }
    static int width(Kind kind,int titleWidth){return Math.max(kind.width,titleWidth+48);}
    static DesktopWindowBounds box(int x,int y,int w,int h){return new DesktopWindowBounds(x,y,w,h);}

    /** Strip transparent atlas gaps; captured world/dimming is never window material.
     * Merge identical row runs vertically to keep the exact pixel mask compact. */
    static List<DesktopWindowBounds> holes(ResourceLocation texture,int sx,int sy,int w,int h) {
        String key=texture+":"+sx+":"+sy+":"+w+":"+h;
        return MASKS.computeIfAbsent(key,ignored->{
            try(var stream=Minecraft.getInstance().getResourceManager().getResource(texture).orElseThrow().open();var image=NativeImage.read(stream)) {
                var result=new ArrayList<DesktopWindowBounds>();
                Map<String,int[]> active=new LinkedHashMap<>();
                for(int y=0;y<h;y++) {
                    Map<String,int[]> next=new LinkedHashMap<>();
                    for(int x=0;x<w;) {
                        if((image.getPixelRGBA(sx+x,sy+y)>>>24)!=0){x++;continue;}
                        int first=x;
                        while(x<w&&(image.getPixelRGBA(sx+x,sy+y)>>>24)==0)x++;
                        String run=first+":"+(x-first);
                        int[] old=active.remove(run);
                        if(old==null)old=new int[]{sx+first,sy+y,x-first,0};
                        old[3]++;next.put(run,old);
                    }
                    for(var r:active.values())result.add(box(r[0],r[1],r[2],r[3]));
                    active=next;
                }
                for(var r:active.values())result.add(box(r[0],r[1],r[2],r[3]));
                return List.copyOf(result);
            }catch(IOException failure){throw new IllegalStateException("Powah artwork mask",failure);}
        });
    }
    private static DesktopNativeRegion masked(String id,ResourceLocation texture,DesktopWindowBounds source,int atlasX,int atlasY,int dx,int dy,boolean exterior) {
        var cuts=holes(texture,atlasX,atlasY,source.width(),source.height()).stream()
            .map(b->box(source.x()+b.x()-atlasX,source.y()+b.y()-atlasY,b.width(),b.height())).toList();
        return new DesktopNativeRegion(id,source,dx,dy,true,cuts,exterior);
    }
    static DesktopNativeLayout layout(Kind kind,int titleWidth) {
        int w=width(kind,titleWidth),bodyX=(w-INSETS.horizontal()-kind.bodyWidth())/2;
        var art=ResourceLocation.fromNamespaceAndPath("powah","textures/gui/container/"+kind.art+".png");
        var builder=DesktopNativeLayout.builder().canvas(w-INSETS.horizontal(),kind.bodyHeight)
            .region(masked("native-machine",art,box(0,0,kind.bodyWidth(),kind.bodyHeight),0,0,bodyX,0,false));
        if(kind!=Kind.CABLE) {
            var buttons=ResourceLocation.fromNamespaceAndPath("lollipop","textures/gui/container/button_ov.png");
            builder.region(masked("native-energy-faces",buttons,box(176,4,23,25),0,0,w-1,INSETS.top()+4,true));
            builder.region(masked("native-redstone",buttons,box(176,30,15,16),23,0,w-1,INSETS.top()+30,true));
        }
        if(kind==Kind.REACTOR)builder.region(masked("native-generation-mode",art,box(-15,6,15,16),209,0,-14,INSETS.top()+6,true));
        return builder.scrollable(false).build();
    }
    public static final class State {final PowahChrome chrome=new PowahChrome();}
    private record Template(Kind kind)implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){return layout(kind,c.fontWidth(c.originalTitle()));}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowDecoration<>() {
                @Override public DesktopChromeMetrics metrics(){return METRICS;}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){draw.state().chrome.paint(draw,controls,kind==Kind.ENDER_CELL);}
            };
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.remove(c.model());c.state().chrome.close();}
        @Override @SuppressWarnings("unchecked")public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.put(c.model(),(DesktopInputContext<AbstractContainerMenu,State>)c);}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "powah-reviewed-native-1:"+kind.path;}
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){PowahFluids.render(c,kind);}
    }
}
