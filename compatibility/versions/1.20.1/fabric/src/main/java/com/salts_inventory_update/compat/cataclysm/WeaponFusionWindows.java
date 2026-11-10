package com.salts_inventory_update.compat.cataclysm;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.nativewindows.RecessedNativeChrome;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native fusion machine inside a color-matched SIU model-preview aperture. */
public final class WeaponFusionWindows {
    private static final ResourceLocation ART=new ResourceLocation("cataclysm","textures/gui/fusion.png");
    private static final String MENU="cataclysm:weapon_fusion";
    private static final int WIDTH=176,HEIGHT=71,WALL_INSET=5;
    private static boolean bound,installed;
    private WeaponFusionWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();RecessedNativeChrome.initialize(mc);});}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type(MENU);if(type==null||bound)return;
        NativeMenuSupport.support(type,"com.github.L_Ender.cataclysm.inventory.WeaponfusionMenu",false);
        DesktopMenuBindings.registerStorage(type,(menu,slot)->false);bound=true;
    }
    private static synchronized void install(){
        var type=NativeMenuSupport.type(MENU);if(type==null||installed)return;
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","cataclysm/recessed-weapon-fusion"),100,Set.of(type),lookup->true,lookup->new Template());installed=true;
    }
    static final class State {
        final transient RecessedNativeChrome skin=new RecessedNativeChrome(70,73,79);
        DesktopWindowBounds oldTitle;
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            // Let the inset walls cover the native frame, including its rounded
            // corners. Equal source/destination offsets leave all slots fixed.
            c.state().oldTitle=new DesktopWindowBounds(66,18,Math.min(108,c.fontWidth(c.originalTitle())+1),9);
            return DesktopNativeLayout.builder().keep("native-fusion-machine",new DesktopWindowBounds(WALL_INSET,WALL_INSET,WIDTH-2*WALL_INSET,HEIGHT-2*WALL_INSET),WALL_INSET,WALL_INSET)
                .hide(c.state().oldTitle).canvas(WIDTH,HEIGHT).scrollable(false).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(9,24,9,9),true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return (draw,controls)->draw.state().skin.paint(draw,controls,WIDTH,HEIGHT,WALL_INSET);}
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            // Restore the exact label-free native circuitry beneath the old
            // centered caption. The single title now belongs to the SIU header.
            var b=c.state().oldTitle;if(b!=null)c.texture(ART,c.contentX()+b.x(),c.contentY()+b.y(),b.x(),b.y(),b.width(),b.height(),b.width(),b.height(),256,256);
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){c.state().skin.close();}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "cataclysm-recessed-native-fusion-2";}
    }
}
