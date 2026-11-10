package com.salts_inventory_update.compat.nativewindows;

import com.mojang.blaze3d.platform.NativeImage;
import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Salt's shell and model-preview bevel, with a genuine unpainted native aperture. */
public final class RecessedNativeChrome implements AutoCloseable {
    private static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/3d_model_display.png");
    private static final ResourceLocation CONTROLS=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
    private static final Set<RecessedNativeChrome> LIVE=Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean listening;
    private final int red,green,blue;
    private transient ResourceLocation frame,rim;
    public RecessedNativeChrome(int red,int green,int blue){this.red=red;this.green=green;this.blue=blue;LIVE.add(this);}
    public static void initialize(Minecraft client){
        if(listening)return;listening=true;
        if(client.getResourceManager() instanceof ReloadableResourceManager resources)
            resources.registerReloadListener((ResourceManagerReloadListener)manager->client.execute(()->{for(var skin:List.copyOf(LIVE))skin.release();}));
    }
    private int channel(int base,int light){return Math.max(0,Math.min(255,light<=198?Math.round(base*light/198f):base+light-198));}
    private ResourceLocation texture(Minecraft client,ResourceLocation source,boolean hole){
        try(var stream=client.getResourceManager().getResource(source).orElseThrow().open();var original=NativeImage.read(stream)){
            var image=new NativeImage(original.getWidth(),original.getHeight(),true);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int c=original.getPixelRGBA(x,y),light=((c&255)+(c>>8&255)+(c>>16&255))/3;
                int color=(c&0xff000000)|channel(blue,light)<<16|channel(green,light)<<8|channel(red,light);
                // The model preview's black center becomes transparent. Its
                // actual 3x3 bevel and corner pixels stay in the new palette.
                if(hole&&x*3/image.getWidth()==1&&y*3/image.getHeight()==1)color=0;
                image.setPixelRGBA(x,y,color);
            }
            return client.getTextureManager().register(hole?"siu_recessed_native_rim":"siu_recessed_native_shell",new DynamicTexture(image));
        }catch(java.io.IOException failure){throw new IllegalStateException("Recessed window skin unavailable",failure);}
    }
    private void ensure(Minecraft client){
        if(frame!=null&&rim!=null)return;
        try{frame=texture(client,DesktopThemeAssets.WINDOW,false);rim=texture(client,MODEL,true);}catch(RuntimeException failure){release();throw failure;}
    }
    private void release(){var textures=Minecraft.getInstance().getTextureManager();if(frame!=null)textures.release(frame);if(rim!=null)textures.release(rim);frame=null;rim=null;}
    @Override public void close(){release();LIVE.remove(this);}
    public void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,int bodyWidth,int bodyHeight){
        paint(c,controls,bodyWidth,bodyHeight,0);
    }
    public void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,int bodyWidth,int bodyHeight,int wallInset){
        ensure(c.minecraft());int x=c.windowX(),y=c.windowY(),w=c.windowWidth(),h=c.windowHeight(),cx=c.contentX()+wallInset,cy=c.contentY()+wallInset;
        bodyWidth-=2*wallInset;bodyHeight-=2*wallInset;
        Runnable shell=()->c.windowNineSlice(frame,x,y,w,h);
        // Draw only the shell surrounding the aperture. No gray backing or
        // black portrait fill is placed behind the original mod's pixels.
        c.withClip(x,y,w,cy-y,shell);
        c.withClip(x,cy,cx-x,bodyHeight,shell);
        c.withClip(cx+bodyWidth,cy,x+w-cx-bodyWidth,bodyHeight,shell);
        c.withClip(x,cy+bodyHeight,w,y+h-cy-bodyHeight,shell);
        c.onePixelNineSlice(rim,cx-1,cy-1,bodyWidth+2,bodyHeight+2);
        header(c,controls);
    }
    /** Filled companion shell, using the same palette as the recessed variant. */
    public void paintSolid(DesktopRenderContext<?,?> c,List<DesktopControlState> controls){
        ensure(c.minecraft());c.windowNineSlice(frame,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        header(c,controls);
    }
    private void header(DesktopRenderContext<?,?> c,List<DesktopControlState> controls){
        int x=c.windowX(),y=c.windowY(),w=c.windowWidth();
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(x+w-6);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-x-7)),x+7,y+5,0xffefeff2,false);
        for(var state:controls){
            int column=switch(state.control()){case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;};
            if(column<0)continue;var b=state.bounds();
            c.texture(CONTROLS,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
