package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Native palettes, sampled from the original container textures.
 * Uses the existing decoration API; window input and management stay with SIU. */
final class ReviewedDarkChrome {
    private ReviewedDarkChrome() { }
    static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Component title,int[] rgb) {
        int x=c.windowX(),y=c.windowY(),w=c.windowWidth(),h=c.windowHeight();
        int background=0xFF000000|(rgb[0]<<16)|(rgb[1]<<8)|rgb[2];
        boolean magicBag=rgb[0]==132&&rgb[1]==132&&rgb[2]==132;
        int highlight=magicBag?0xFFDFDFDF:0xFF5D595E;
        int shadow=magicBag?0xFF555555:0xFF3F3A41;
        // Rounded SIU silhouette; the face matches the source texture exactly.
        c.fill(x+2,y,x+w-3,y+h,0xFF000000);c.fill(x+1,y+1,x+w-1,y+h-1,0xFF000000);
        c.fill(x,y+2,x+w,y+h-3,0xFF000000);
        c.fill(x+2,y+1,x+w-4,y+h-4,highlight);c.fill(x+1,y+2,x+w-4,y+h-4,highlight);
        c.fill(x+3,y+3,x+w-3,y+h-3,shadow);c.fill(x+3,y+3,x+w-5,y+h-5,background);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP)
            .mapToInt(s->s.bounds().x()-3).min().orElse(x+w-6);
        c.text(c.trimToWidth(title.getString(),Math.max(0,right-x-6)),x+6,y+5,magicBag?0xFF202020:0xFFE8E6E9,false);
        var texture=Identifier.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
        for(var state:controls) {
            int column=switch(state.control()) {
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(column<0)continue;
            var b=state.bounds();
            c.texture(texture,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,
                b.width(),b.height(),11,11,132,33);
        }
    }
}
