package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import static com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts.box;

/** Reviewed 1.20.1 native artwork: every retained real slot maps to exactly one complete cell. */
public final class NativeBatchLayouts {
    private NativeBatchLayouts() { }
    private static DesktopNativeLayout checked(DesktopNativeView view,DesktopNativeLayout layout) {
        if(view.imageBounds().width()!=176) return NativeWindowPolish.machine(view);
        for(var slot:view.slots()) if(!slot.player()&&slot.active()&&layout.placements(slot.bounds()).size()!=1)
            return NativeWindowPolish.machine(view);
        return layout;
    }
    private static DesktopNativeLayout crop(DesktopNativeView view,int x,int y,int w,int h) {
        return checked(view,DesktopNativeLayout.builder().keep("body",box(x,y,w,h),0,0).build());
    }
    public static DesktopNativeLayout drawers(DesktopNativeView view) {
        // Preview cells are deliberately inactive upstream but are still painted by DrawerScreen.
        int count=(int)view.slots().stream().filter(s->!s.player()).count();
        if(count!=6&&count!=8&&count!=12)return NativeWindowPolish.machine(view);
        int left=view.slotBounds(0).map(DesktopWindowBounds::x).orElse(52);
        if(count==6) {
            int x=Math.min(70,left),right=Math.max(106,left+(left==52?72:54));
            return crop(view,x,18,right-x,53);
        }
        return crop(view,50,count==12?8:18,76,count==12?63:53);
    }
    public static DesktopNativeLayout ossuary(DesktopNativeView view) { return crop(view,19,14,141,57); }
    public static DesktopNativeLayout smelter(DesktopNativeView view) { return crop(view,19,16,134,56); }
    public static DesktopNativeLayout combiner(DesktopNativeView view) { return crop(view,19,16,102,56); }
    public static DesktopNativeLayout runes(DesktopNativeView view) { return crop(view,26,46,126,19); }
    public static DesktopNativeLayout fletching(DesktopNativeView view) { return crop(view,29,16,125,55); }
    public static DesktopNativeLayout chunkLoader(DesktopNativeView view) { return crop(view,4,5,165,67); }
    public static DesktopNativeLayout anvil(DesktopNativeView view) {
        // Move the text field intact. Reserve the complete native repair-cost line without
        // baking the lower Inventory label into the empty-state artwork.
        return tabbed(view,DesktopNativeLayout.builder().canvas(161,54)
            .region(new DesktopNativeRegion("name",box(61,23,105,14),28,0,false,List.of()))
            .keep("items",box(8,45,161,22),0,20).build());
    }
    public static DesktopNativeLayout reforge(DesktopNativeView view) {
        return tabbed(view,DesktopNativeLayout.builder().canvas(88,41)
            .keep("base",box(44,46,18,18),0,13)
            .keep("gear",box(79,33,18,18),35,0)
            .keep("addition",box(114,46,18,18),70,13)
            .keep("action",box(79,56,18,18),35,23).build());
    }
    public static DesktopNativeLayout smithing(DesktopNativeView view) {
        // The stand is a live 3D preview, not unused padding. Avoid recapturing the native heading/hammer.
        // Vanilla renders the stand around (141,75), not with its left edge at 139.
        // Include both arms, head/feet and room for the equipped result's silhouette.
        return tabbed(view,DesktopNativeLayout.builder().canvas(172,60)
            .keep("items",box(7,47,109,19),0,27)
            .keep("preview",box(117,20,56,60),116,0).build());
    }
    public static DesktopNativeLayout linking(DesktopNativeView view) {
        var gems=view.slots().stream().filter(s->!s.player()&&s.menuSlotId()>=2).map(DesktopNativeView.Slot::bounds).toList();
        var strip=DesktopNativeView.union(gems).orElse(box(34,42,108,18));
        // Configured gem counts retain their native geometry; widen rather than silently lose sockets.
        int left=Math.min(34,strip.x()),right=Math.max(166,strip.x()+strip.width());
        return tabbed(view,DesktopNativeLayout.builder().canvas(right-left,42)
            .keep("gear",box(79,18,18,18),79-left,0)
            .keep("addition",box(105,18,18,18),105-left,0)
            .keep("action",box(148,18,18,18),148-left,0)
            .keep("sockets",strip,strip.x()-left,24).build());
    }
    public static DesktopNativeLayout grindstone(DesktopNativeView view) { return tabbed(view,crop(view,30,15,116,59)); }
    public static DesktopNativeLayout unlinking(DesktopNativeView view) { return tabbed(view,crop(view,30,15,116,59)); }
    private static DesktopNativeLayout tabbed(DesktopNativeView view,DesktopNativeLayout body) {
        var checked=checked(view,body);
        if(checked!=body)return checked;
        // LibZ's first inactive tab continues four rows inside the native frame.
        // Keep those rows behind Salt's rounded corner rather than exposing the world.
        return body.toBuilder().exterior("tabs",box(0,-24,49,28),0,-24).build();
    }
    /** Keep a compact body centered when the complete title needs a wider frame. */
    public static DesktopNativeLayout center(DesktopNativeLayout layout,int minimumWidth) {
        int width=Math.max(layout.width(),minimumWidth),shift=(width-layout.width())/2;
        if(shift==0&&width==layout.width())return layout;
        return new DesktopNativeLayout(layout.regions().stream()
            .map(r->r.exterior()?r:r.at(r.x()+shift,r.y())).toList(),layout.padding(),width,layout.minimumHeight(),layout.scrollable());
    }
}
