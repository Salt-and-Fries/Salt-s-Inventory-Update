package com.salts_inventory_update.compat.kibe;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.kibe.KibeWindows.Theme;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Exact approved SIU geometry with Kibe's native cyan, teal, gold and charcoal. */
final class KibeChrome implements AutoCloseable {
    private static final Set<KibeChrome> LIVE=Collections.newSetFromMap(new WeakHashMap<>());
    private static final ResourceLocation CONTROLS=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
    private ResourceLocation frame;
    KibeChrome(){LIVE.add(this);}
    static void reload(){for(var chrome:List.copyOf(LIVE))chrome.release();}
    private void ensure(Theme theme){
        if(frame!=null)return;
        int[] palette=switch(theme){
            case GRAY->new int[]{0x000000,0x333333,0x555555,0x8b8b8b,0xc6c6c6,0xffffff};
            case COOLER->new int[]{0x000000,0x004646,0x007e7e,0x009195,0x00ced2,0x00ffff};
            case TEAL->new int[]{0x000000,0x0e2b2b,0x0d5253,0x194e4e,0x1c7a7b,0x3db7b7};
            case DRAWBRIDGE->new int[]{0x000000,0x4b3d0d,0x7c5e00,0xb99420,0x4c4c4c,0xffe000};
            case TRASH->new int[]{0x000000,0x232323,0x333333,0x464646,0x4c4c4c,0x7a7a7a};
        };
        try(var stream=Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window.png")).orElseThrow().open();
            var original=NativeImage.read(stream)){
            int[] levels={0,51,85,139,198,255};
            var result=new NativeImage(original.getWidth(),original.getHeight(),true);
            for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++){
                int value=original.getPixelRGBA(x,y),light=value&255,nearest=0;
                for(int i=1;i<levels.length;i++)if(Math.abs(light-levels[i])<Math.abs(light-levels[nearest]))nearest=i;
                int rgb=palette[nearest],abgr=(value&0xff000000)|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>>16)&255);
                result.setPixelRGBA(x,y,abgr);
            }
            frame=Minecraft.getInstance().getTextureManager().register("siu_kibe_window",new DynamicTexture(result));
        }catch(IOException failure){throw new IllegalStateException("Kibe window frame",failure);}
    }
    void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Theme theme){
        ensure(theme);int x=c.windowX(),y=c.windowY();
        c.windowNineSlice(frame,x,y,c.windowWidth(),c.windowHeight());
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP)
            .mapToInt(s->s.bounds().x()-7).min().orElse(x+c.windowWidth()-7);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-x-7)),x+7,y+7,
            theme==Theme.GRAY?0xff404040:theme==Theme.DRAWBRIDGE?0xffffe000:0xffffffff,false);
        for(var state:controls){
            int column=switch(state.control()){
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->state.selected()?5:4;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(column<0)continue;var b=state.bounds();
            c.texture(CONTROLS,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
    private void release(){if(frame!=null){Minecraft.getInstance().getTextureManager().release(frame);frame=null;}}
    @Override public void close(){release();LIVE.remove(this);}
}
