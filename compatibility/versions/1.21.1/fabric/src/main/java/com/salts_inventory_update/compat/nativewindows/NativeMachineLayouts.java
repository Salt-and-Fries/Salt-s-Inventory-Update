package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;

/** Reviewed native artwork crops. Slots keep their original identity and behavior. */
public final class NativeMachineLayouts {
    private NativeMachineLayouts() { }

    public static DesktopWindowBounds box(int x, int y, int width, int height) {
        return new DesktopWindowBounds(x,y,width,height);
    }

    private static DesktopNativeLayout crop(DesktopNativeView view, DesktopWindowBounds body) {
        for (var slot : view.slots()) {
            if (slot.player() || !slot.active()) continue;
            var cell=slot.bounds();
            if (cell.x()<body.x() || cell.y()<body.y()
                    || cell.x()+cell.width()>body.x()+body.width()
                    || cell.y()+cell.height()>body.y()+body.height()) return NativeWindowPolish.machine(view);
        }
        return DesktopNativeLayout.builder().keep("body",body,0,0).build();
    }

    public static DesktopNativeLayout bartering(DesktopNativeView view) {
        // Gold, piglin model/count, both cooldown arrows and the returned-item grid.
        return crop(view,box(10,16,156,56));
    }

    public static DesktopNativeLayout identifying(DesktopNativeView view) {
        return crop(view,box(25,19,127,47));
    }

    public static DesktopNativeLayout fossil(DesktopNativeView view) {
        // Forge appends the fossil socket after all 36 player slots; Fabric puts it first.
        var cell=view.slots().stream().filter(slot->slot.active()&&!slot.player())
                .map(DesktopNativeView.Slot::bounds).findFirst().orElse(null);
        if(cell==null)return NativeWindowPolish.machine(view);
        // The original fossil socket is a 32px beveled plate around an 18px cell.
        return crop(view,box(cell.x()-7,cell.y()-7,cell.width()+14,cell.height()+14));
    }

    public static DesktopNativeLayout imbuing(DesktopNativeView view) {
        // The native scroll illustration is part of the workbench, not a slot.
        return crop(view,box(25,10,129,62));
    }

    public static DesktopNativeLayout combustion(DesktopNativeView view) {
        return crop(view,box(43,13,91,55));
    }

    public static DesktopNativeLayout sawmill(DesktopNativeView view) {
        // Recipe tiles/scrollbar are not widgets. Keep their complete native canvas,
        // including the search field when its threshold/configuration enables it.
        boolean wide=view.slotBounds(0).map(cell->cell.x()==16).orElse(false);
        return crop(view,wide?box(16,13,151,58):box(20,13,144,58));
    }

    public static DesktopNativeLayout cooking(DesktopNativeView view) {
        return cooking(view,box(29,16,115,57));
    }

    public static DesktopNativeLayout copperPot(DesktopNativeView view) {
        return cooking(view,box(39,17,98,57));
    }

    private static DesktopNativeLayout cooking(DesktopNativeView view,DesktopWindowBounds body) {
        var cropped=crop(view,body);
        if(!cropped.regions().get(0).source().equals(body))return cropped;
        var layout=cropped.toBuilder();
        for(var widget:view.widgets()) {
            if(widget.visible() && widget.bounds().width()==20 && widget.bounds().height()==18
                    && widget.bounds().x()<body.x()) {
                // A compact left column keeps the original book button inside
                // the frame, separated from the native ingredients by four pixels.
                layout.moveRegion("body",24,0)
                        .keep("recipe-toggle",widget.bounds(),0,widget.bounds().y()-body.y());
                break;
            }
        }
        // Core hosts the original recipe component and routes this native button
        // to that one presentation. Remove the native lower inventory label's edge.
        layout.hide(box(29,72,40,8));
        return layout.build();
    }

    public static DesktopNativeLayout alloyForge(DesktopNativeView view) {
        var layout=crop(view,box(5,21,160,71)).toBuilder();
        // Owo draws the lava reservoir as a small tab above the main forge body.
        return layout.hide(box(30,21,105,13))
                .exterior("lava-reservoir",box(57,0,66,18),55,-16).build();
    }

    public static DesktopNativeLayout alloyed(DesktopNativeView view) {
        return crop(view,box(29,14,116,57));
    }

    public static DesktopNativeLayout conditioning(DesktopNativeView view) {
        // Nine ingredients, three fuel/module cells, valve switches and indicators.
        return crop(view,box(25,16,129,55)).toBuilder()
                .keep("particles",box(163,6,7,7),122,0).build();
    }
}
