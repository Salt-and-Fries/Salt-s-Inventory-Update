package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModernSmelterLayoutTest {
    private static DesktopNativeView view() {
        var slots=new ArrayList<DesktopNativeView.Slot>();
        for(var p:new int[][]{{45,17},{67,17},{56,53},{129,35}})
            slots.add(new DesktopNativeView.Slot(slots.size(),new DesktopWindowBounds(p[0]-1,p[1]-1,18,18),false,true));
        slots.add(new DesktopNativeView.Slot(4,new DesktopWindowBounds(7,83,18,18),true,true));
        return new DesktopNativeView("modern-smelter",new DesktopWindowBounds(0,0,176,166),
            new DesktopWindowBounds(19,16,134,56),slots,List.of());
    }
    @Test void nativeMachineAndCompleteOutputKeepTheirGeometryWithoutTheRemovedBookColumn() {
        var view=view();var layout=NativeBatchLayouts.smelter(view);
        assertEquals(109,layout.width());
        assertEquals(44,layout.regions().get(0).source().x());
        for(var slot:view.slots()) {
            var placed=layout.placements(slot.bounds());
            if(slot.player()){assertTrue(placed.isEmpty());continue;}
            assertEquals(1,placed.size());var bounds=placed.get(0).bounds();
            var source=layout.sourceAt(bounds.x()+9,bounds.y()+9).orElseThrow();
            assertEquals(slot.bounds().x()+9,source.x());assertEquals(slot.bounds().y()+9,source.y());
        }
        assertTrue(layout.placements(new DesktopWindowBounds(19,34,20,18)).isEmpty());
        for(var art:List.of(new DesktopWindowBounds(56,36,14,14),new DesktopWindowBounds(92,34,25,16),
                new DesktopWindowBounds(124,30,28,28)))assertEquals(1,layout.placements(art).size());
    }
}
