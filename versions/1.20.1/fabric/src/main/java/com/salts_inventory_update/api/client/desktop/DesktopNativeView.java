package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Read-only native geometry. No screen implementation, private field or native widget is exposed. */
public record DesktopNativeView(String screenType, DesktopWindowBounds imageBounds,
        DesktopWindowBounds automaticBounds, List<Slot> slots, List<Widget> widgets) {
    public DesktopNativeView { slots=List.copyOf(slots);widgets=List.copyOf(widgets); }
    public record Slot(int menuSlotId, DesktopWindowBounds bounds, boolean player, boolean active) { }
    /** IDs are local handles rebuilt with native widgets; match labels/bounds when upstream layouts vary. */
    public record Widget(String id, String type, Component label, DesktopWindowBounds bounds, boolean visible, boolean active) { }
    public Optional<DesktopWindowBounds> slotBounds(int... ids) {
        return union(slots.stream().filter(s->java.util.Arrays.stream(ids).anyMatch(id->id==s.menuSlotId())).map(Slot::bounds).toList());
    }
    public Optional<DesktopWindowBounds> widgetBounds(String... ids) {
        return union(widgets.stream().filter(w->java.util.Arrays.asList(ids).contains(w.id())).map(Widget::bounds).toList());
    }
    public static Optional<DesktopWindowBounds> union(List<DesktopWindowBounds> bounds) {
        if(bounds.isEmpty())return Optional.empty();int x=Integer.MAX_VALUE,y=Integer.MAX_VALUE,r=Integer.MIN_VALUE,b=Integer.MIN_VALUE;
        for(var a:bounds){x=Math.min(x,a.x());y=Math.min(y,a.y());r=Math.max(r,a.x()+a.width());b=Math.max(b,a.y()+a.height());}
        return Optional.of(new DesktopWindowBounds(x,y,r-x,b-y));
    }
}
