package com.salts_inventory_update.compat.bumblezone;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** SIU frame pixels recolored to the original honey/wood materials. */
final class BumblezoneChrome {
    private static final Identifier HONEY=Identifier.fromNamespaceAndPath("salts_inventory_compat","textures/gui/bumblezone/honey_window.png");
    private static final Identifier WOOD=Identifier.fromNamespaceAndPath("salts_inventory_compat","textures/gui/bumblezone/wood_window.png");
    private static final Identifier CONTROLS=Identifier.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
    private BumblezoneChrome() { }
    static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Component title,boolean briefcase) {
        int x=c.windowX(),y=c.windowY(),w=c.windowWidth();
        c.windowNineSlice(briefcase?WOOD:HONEY,x,y,w,briefcase?23:c.windowHeight());
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(x+w-6);
        c.text(c.trimToWidth(title.getString(),Math.max(0,right-x-7)),x+7,y+5,briefcase?0xFFF5E2BF:0xFF49341D,false);
        for(var state:controls) {
            int column=switch(state.control()) {
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(column<0)continue;var b=state.bounds();
            int row=state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0;
            c.texture(CONTROLS,b.x(),b.y(),column*11,row*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
