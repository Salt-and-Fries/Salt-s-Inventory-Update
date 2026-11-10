package com.salts_inventory_update.compat.adastra;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Approved native metal housings, complete live apparatus and attached tools. */
public final class AdAstraClient {
    private static final Set<String> INSTALLED=new HashSet<>();
    private static final Map<ResourceLocation,Integer> COLORS=new HashMap<>();
    private static final ResourceLocation CONTROLS=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
    private static boolean listening;
    private AdAstraClient() { }
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{install();listen(mc);});}
    private static synchronized void install(){
        AdAstraWindows.bind();
        for(var spec:AdAstraSpec.all()){
            var type=NativeMenuSupport.type(spec.menu);
            if(type==null||!INSTALLED.add(spec.menu))continue;
            DesktopClientApi.registerNativeMatcher(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","ad-astra-native/"+spec.id),120,
                Set.of(type),lookup->true,lookup->new Template(spec));
        }
    }
    private static void listen(Minecraft mc){
        if(listening)return;listening=true;
        if(mc.getResourceManager() instanceof ReloadableResourceManager manager)
            manager.registerReloadListener((ResourceManagerReloadListener)resources->COLORS.clear());
    }
    static final class State {
        boolean configuring,virtualConfiguration;
        DesktopWindowBounds configuration;
        int drawerHeight;
        List<DesktopWindowBounds> drawerCells=List.of();
    }
    private record Template(AdAstraSpec spec) implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){AdAstraScreenAccess.prepare(c.model(),spec);}
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c){
            return c.originalTitle().getString().isBlank()?Component.literal(spec.title):c.originalTitle();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,spec.header,0,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            var view=c.nativeView();var body=spec.body();
            var players=view.slots().stream().filter(DesktopNativeView.Slot::player).toList();
            var state=c.state();state.virtualConfiguration=players.isEmpty();
            state.configuring=!spec.vehicle&&AdAstraScreenAccess.configuring(c.model());
            var nativeBody=new DesktopNativeRegion("native-working-apparatus",body,0,0);
            // Restore the atlas underneath former labels, including the first
            // few pixels of an old title/Inventory label crossing an extraction.
            if(spec.plate==0)nativeBody=nativeBody.cut(new DesktopWindowBounds(-3,spec.titleY,11+c.fontWidth(c.originalTitle()),9));
            if(spec.labelY>=0)nativeBody=nativeBody.cut(new DesktopWindowBounds(-3,spec.labelY,
                state.configuring?spec.width-4:11+c.fontWidth(Component.translatable("container.inventory")),9));
            var plan=DesktopNativeLayout.builder().canvas(spec.width,spec.height-spec.header-6).scrollable(false).region(nativeBody);
            for(var f:spec.fragments){
                var r=new DesktopNativeRegion(f.id,AdAstraSpec.box(f.source),f.at[0],f.at[1],true,List.of(),true);
                for(var hole:f.cutouts)r=r.cut(AdAstraSpec.box(hole));
                plan.region(r);
            }
            if(state.configuring){
                var grid=state.virtualConfiguration?new DesktopWindowBounds(7,spec.textureHeight+20,162,76):
                    DesktopNativeView.union(players.stream().map(DesktopNativeView.Slot::bounds).toList()).orElseThrow();
                state.configuration=new DesktopWindowBounds(grid.x(),grid.y()-11,grid.width(),grid.height()+12);
                state.drawerHeight=state.configuration.height()+14;
                if(state.virtualConfiguration){
                    var cells=new ArrayList<DesktopWindowBounds>();
                    int index=0;
                    for(var r:AdAstraScreenAccess.configurationCells(c.model())){
                        int dx=(spec.width-grid.width())/2+r.x()-grid.x(),dy=spec.height+9+r.y()-state.configuration.y();
                        plan.exterior("native-side-configuration/"+index++,r,dx,dy);
                        cells.add(new DesktopWindowBounds(dx,dy-spec.height-2,r.width(),r.height()));
                    }
                    state.drawerCells=List.copyOf(cells);
                }else plan.exterior("native-side-configuration",state.configuration,(spec.width-grid.width())/2,spec.height+9);
            }
            return plan.build();
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowDecoration<>(){
                @Override public DesktopChromeMetrics metrics(){return new DesktopChromeMetrics(spec.header,7,spec.plate>0?5:7,11,11,3,5+spec.reserve,spec.plate>0?3:5,0);}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){
                    frame(draw,spec,draw.windowX(),draw.windowY(),spec.height);
                    int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-6).min().orElse(draw.windowX()+spec.width-7);
                    draw.text(draw.trimToWidth(title(draw).getString(),Math.max(0,right-draw.windowX()-7)),draw.windowX()+7,draw.windowY()+(spec.plate>0?5:7),spec.titleColor,false);
                    paintControls(draw,controls);
                }
            };
        }
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            var b=spec.body();
            c.texture(ResourceLocation.parse(spec.texture),c.contentX(),c.contentY(),0,b.y(),spec.width,b.height(),spec.width,b.height(),spec.width,spec.textureHeight);
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,
                com.salts_inventory_update.api.client.input.MouseButtonEvent event,boolean twice){
            if(!c.state().configuring)return false;
            var context=(DesktopNativeContext<AbstractContainerMenu,State>)c;var layout=nativeLayout(context);
            for(var w:context.nativeView().widgets())if(w.active()&&w.visible()
                    &&(w.type().endsWith(".SlotWidget")||w.type().endsWith(".EnergyBarWidget")||w.type().endsWith(".FluidBarWidget")))
                for(var place:layout.placements(w.bounds())){
                    var r=place.bounds();int x=c.contentX()+r.x(),y=c.contentY()+r.y();
                    if(event.x()>=x&&event.x()<x+r.width()&&event.y()>=y&&event.y()<y+r.height()){
                        AdAstraScreenAccess.select(c.model(),w.bounds().x(),w.bounds().y());return true;
                    }
                }
            return false;
        }
        @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c){
            return c.state() instanceof State s&&s.configuring?List.of(DesktopWindowRegion.visual(0,spec.height+2,spec.width,s.drawerHeight)):List.of();
        }
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){
            if(!(c.state() instanceof State s)||!s.configuring)return;
            int x=c.windowX(),y=c.windowY()+spec.height+2,h=s.drawerHeight;
            // Native exterior content has already painted. Finish only its
            // seven-pixel perimeter, without painting over any native control.
            for(var r:List.of(new DesktopWindowBounds(x,y,spec.width,7),new DesktopWindowBounds(x,y+h-7,spec.width,7),
                    new DesktopWindowBounds(x,y+7,7,h-14),new DesktopWindowBounds(x+spec.width-7,y+7,7,h-14)))
                c.withClip(r.x(),r.y(),r.width(),r.height(),()->frame(c,spec,x,y,h));
            if(s.virtualConfiguration){
                var face=new DesktopNativeRegion("configuration-face",new DesktopWindowBounds(7,7,spec.width-14,h-14),0,0,true,s.drawerCells);
                for(var piece:face.pieces())c.fill(x+piece.x(),y+piece.y(),x+piece.x()+piece.width(),y+piece.y()+piece.height(),AdAstraClient.surface(spec));
                c.text(AdAstraScreenAccess.configurationTitle((AbstractContainerMenu)c.model()),x+7,y+7,spec.titleColor,false);
            }
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "ad-astra-native-approved-1:"+spec.id;}
    }
    private static int surface(AdAstraSpec spec){
        var texture=ResourceLocation.parse(spec.texture);
        return COLORS.computeIfAbsent(texture,id->{
            try(var stream=Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open();var im=NativeImage.read(stream)){
                int abgr=im.getPixelRGBA(7,spec.plate+16);return (abgr&0xff00ff00)|((abgr&255)<<16)|((abgr>>>16)&255);
            }catch(IOException error){throw new IllegalStateException("Ad Astra native material",error);}
        });
    }
    private static void frame(DesktopRenderContext<?,?> c,AdAstraSpec s,int x,int y,int h){
        var texture=ResourceLocation.parse(s.texture);int w=s.width;
        c.fill(x,y,x+w,y+h,surface(s));
        c.texture(texture,x,y,0,s.plate,w,6,w,6,w,s.textureHeight);
        c.texture(texture,x,y+h-6,0,s.textureHeight-6,w,6,w,6,w,s.textureHeight);
        for(int edge:new int[]{0,w-6})c.texture(texture,x+edge,y+6,edge,s.plate+16,6,h-12,6,1,w,s.textureHeight);
        if(s.plate>0)c.texture(texture,x,y,0,s.plate,w,16,w,16,w,s.textureHeight);
    }
    private static void paintControls(DesktopRenderContext<?,?> c,List<DesktopControlState> controls){
        for(var state:controls){
            int col=switch(state.control()){
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->state.selected()?5:4;case PIN->state.selected()?7:6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(col<0)continue;var b=state.bounds();
            c.texture(CONTROLS,b.x(),b.y(),col*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,
                b.width(),b.height(),11,11,132,33);
        }
    }
}
