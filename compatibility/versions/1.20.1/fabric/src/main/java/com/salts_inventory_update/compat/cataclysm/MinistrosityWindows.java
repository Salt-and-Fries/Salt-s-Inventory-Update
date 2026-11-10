package com.salts_inventory_update.compat.cataclysm;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.RecessedNativeChrome;
import com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native gold companion chest and live portrait in the approved brown shell. */
public final class MinistrosityWindows {
    private static final ResourceLocation ART=new ResourceLocation("cataclysm","textures/gui/ministrosity2.png");
    private MinistrosityWindows() { }
    public static void initializeClient(){DesktopClientApi.onClientStarted(mc->{
        var type=ReviewedEntityInventories.typeFor("com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu");
        if(type==null)return;
        RecessedNativeChrome.initialize(mc);
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","cataclysm/ministrosity"),100,Set.of(type),lookup->true,lookup->new Template());
    });}
    static final class State {
        final transient RecessedNativeChrome skin=new RecessedNativeChrome(59,51,45);
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            // Capture only the real cells. The original live pet can overlap
            // the chest's transparent left edge, so draw its clean atlas below.
            return DesktopNativeLayout.builder().canvas(190,86).scrollable(false)
                .keep("native-gold-slots",new DesktopWindowBounds(70,17,90,54),81,21).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,20,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return (draw,controls)->draw.state().skin.paintSolid(draw,controls);}
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            int x=c.contentX()+64,y=c.contentY()+4;
            Runnable chest=()->c.texture(ART,x,y,53,0,123,76,123,76,256,256);
            // Exclude the two native player-inventory joins without trimming
            // the chest's ears, lid or lower frame.
            c.withClip(x,y,123,28,chest);
            c.withClip(x+9,y+28,106,48,chest);
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){
            int x=c.contentX(),y=c.contentY();
            // Close only the former inventory joins, as in the reviewed mockup.
            c.fill(x+73,y+32,x+74,y+80,0xff000000);
            c.fill(x+178,y+32,x+179,y+80,0xff000000);
            c.fill(x+73,y+79,x+179,y+80,0xff000000);
            // A uniform dark rim has no white edge on any side of the portrait.
            c.fill(x+4,y+6,x+59,y+81,0xff292521);
            c.fill(x+5,y+7,x+58,y+80,0xff181617);
            var owner=ReviewedEntityInventories.owner(c.model());
            if(owner instanceof LivingEntity entity&&entity.isAlive())c.withClip(x+5,y+7,53,73,()->
                InventoryScreen.renderEntityInInventoryFollowsMouse(c.nativeGraphics(),x+37,y+58,36,
                    (float)(x+31-c.mouseX()),(float)(y+43-c.mouseY()),entity));
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){c.state().skin.close();}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "cataclysm-ministrosity-brown-companion-1";}
    }
}
