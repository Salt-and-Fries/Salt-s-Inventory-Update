package com.salts_inventory_update.compat.powah;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** SIU's exact frame silhouette with the approved native gray/teal palette. */
final class PowahChrome implements AutoCloseable {
    private static final Set<PowahChrome> LIVE=Collections.newSetFromMap(new WeakHashMap<>());
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private ResourceLocation frame;
    PowahChrome(){LIVE.add(this);}
    static void reload(){for(var chrome:List.copyOf(LIVE))chrome.release();}
    private NativeImage image(String namespace,String path)throws IOException {
        try(var stream=Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(namespace,path)).orElseThrow().open()){return NativeImage.read(stream);}
    }
    private void ensure(boolean ender) {
        if(frame!=null)return;
        try(var original=image("salts_inventory_update","textures/gui/window.png");
            var blank=image("lollipop","textures/gui/container/blank.png");
            var teal=image("lollipop","textures/gui/container/ender_blank.png");
            var widget=image("lollipop","textures/gui/container/ender_widget.png")) {
            int[] levels={0,51,85,139,198,255};
            int[] colors={widget.getPixelRGBA(0,0),teal.getPixelRGBA(19,19),teal.getPixelRGBA(20,20),
                widget.getPixelRGBA(2,2),teal.getPixelRGBA(20,20),widget.getPixelRGBA(1,1)};
            var result=new NativeImage(original.getWidth(),original.getHeight(),true);
            for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
                int value=original.getPixelRGBA(x,y),light=value&255,color=value;
                if(ender) {
                    int nearest=0;
                    for(int i=1;i<levels.length;i++)if(Math.abs(light-levels[i])<Math.abs(light-levels[nearest]))nearest=i;
                    color=(value&0xff000000)|(colors[nearest]&0xffffff);
                }else if((value&0xffffff)==0xc6c6c6)color=(value&0xff000000)|(blank.getPixelRGBA(20,20)&0xffffff);
                result.setPixelRGBA(x,y,color);
            }
            frame=Minecraft.getInstance().getTextureManager().register("siu_powah_window",new DynamicTexture(result));
        }catch(IOException failure){throw new IllegalStateException("Powah window palette",failure);}
    }
    void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,boolean ender) {
        ensure(ender);int x=c.windowX(),y=c.windowY();
        c.windowNineSlice(frame,x,y,c.windowWidth(),c.windowHeight());
        int right=controls.stream().mapToInt(s->s.bounds().x()-7).min().orElse(x+c.windowWidth()-7);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-x-7)),x+7,y+7,ender?0xffc2ece8:0xff303030,false);
        for(var state:controls) {
            int column=switch(state.control()) {
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->state.selected()?5:4;case PIN->6;
                case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(column<0)continue;var b=state.bounds();
            c.texture(CONTROLS,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
    private void release(){if(frame!=null){Minecraft.getInstance().getTextureManager().release(frame);frame=null;}}
    @Override public void close(){release();LIVE.remove(this);}
}
