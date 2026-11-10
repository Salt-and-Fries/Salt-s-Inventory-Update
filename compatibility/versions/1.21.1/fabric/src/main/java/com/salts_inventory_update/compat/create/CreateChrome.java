package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Shared SIU management sprites. Every frame and body now remains native Create artwork. */
final class CreateChrome {
    private CreateChrome() { }
    static void controls(DesktopRenderContext<?,?> c,List<DesktopControlState> controls) {
        var texture=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
        for(var state:controls) {
            int col=switch(state.control()) {
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->state.selected()?5:4;case PIN->state.selected()?7:6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            if(col<0)continue;var b=state.bounds();
            c.texture(texture,b.x(),b.y(),col*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
