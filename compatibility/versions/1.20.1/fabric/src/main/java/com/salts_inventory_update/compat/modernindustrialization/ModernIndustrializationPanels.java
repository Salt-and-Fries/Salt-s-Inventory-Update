package com.salts_inventory_update.compat.modernindustrialization;

import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.resources.ResourceLocation;

/** Restore the side tray's inner wall wherever the parent no longer covers it. */
public final class ModernIndustrializationPanels {
    private static final ResourceLocation FRAME=new ResourceLocation("salts_inventory_update","textures/gui/window.png");
    private static final ResourceLocation NATIVE=new ResourceLocation("modern_industrialization","textures/gui/container/background.png");
    private ModernIndustrializationPanels() { }
    public static void paint(DesktopRenderContext<?,?> c,DesktopNativeLayout layout){
        if(layout==null)return;
        for(var region:layout.regions())if(region.exterior()){
            boolean left=region.id().equals("native-shape-tray");
            int x=c.windowX()+region.x(),y=c.windowY()+region.y(),w=region.source().width(),h=region.source().height();
            int edge=left?x+w-4:x,start=y+4,end=y+h,parentBottom=c.windowY()+c.windowHeight();
            int jointEnd=Math.min(end,parentBottom);
            if(jointEnd>start)c.fill(edge,start,edge+4,jointEnd,0xFFC6C6C6);
            if(end<=parentBottom){
                // The native open join ends in a horizontal bottom border.
                c.texture(NATIVE,edge,end-4,100,252,4,4,1,4,256,256);
            }else{
                int exposed=Math.max(start,parentBottom);
                if(end>exposed)c.withClip(edge,exposed,4,end-exposed,()->c.windowNineSlice(FRAME,x,y,w,h));
            }
        }
    }
}
