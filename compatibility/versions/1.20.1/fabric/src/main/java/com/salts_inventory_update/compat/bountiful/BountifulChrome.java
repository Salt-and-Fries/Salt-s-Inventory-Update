package com.salts_inventory_update.compat.bountiful;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** SIU silhouette using the exact colors sampled from Bountiful's board atlas. */
final class BountifulChrome {
    static final int FACE=0xFF563426,TEXT=0xFFEADAB5;
    private static final int LIGHT=0xFF794A36,SHADOW=0xFF21140F;
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private BountifulChrome() { }
    static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls){
        int x=c.windowX(),y=c.windowY(),w=c.windowWidth(),h=c.windowHeight();
        c.fill(x+2,y,x+w-3,y+h,0xFF000000);c.fill(x+1,y+1,x+w-1,y+h-1,0xFF000000);c.fill(x,y+2,x+w,y+h-3,0xFF000000);
        c.fill(x+2,y+1,x+w-4,y+h-4,LIGHT);c.fill(x+1,y+2,x+w-4,y+h-4,LIGHT);
        c.fill(x+3,y+3,x+w-3,y+h-3,SHADOW);c.fill(x+3,y+3,x+w-5,y+h-5,FACE);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(x+w-6);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-x-6)),x+6,y+5,TEXT,false);
        for(var state:controls){
            int column=switch(state.control()){
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(column<0)continue;var b=state.bounds();int row=state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0;
            c.texture(CONTROLS,b.x(),b.y(),column*11,row*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
