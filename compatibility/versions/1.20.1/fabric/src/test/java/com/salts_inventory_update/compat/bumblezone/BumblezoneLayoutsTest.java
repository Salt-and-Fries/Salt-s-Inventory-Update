package com.salts_inventory_update.compat.bumblezone;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BumblezoneLayoutsTest {
    private DesktopNativeView view(List<DesktopNativeView.Slot> slots) {
        var bounds=new DesktopWindowBounds(0,0,176,196);
        return new DesktopNativeView("reference",bounds,bounds,slots,List.of());
    }
    private void complete(DesktopNativeLayout layout,DesktopWindowBounds source) {
        var placements=layout.placements(source);assertEquals(1,placements.size());
        var target=placements.get(0).bounds();
        for(int[] corner:new int[][]{{0,0},{source.width()-1,0},{0,source.height()-1},{source.width()-1,source.height()-1}}) {
            var inverse=layout.sourceAt(target.x()+corner[0],target.y()+corner[1]).orElseThrow();
            assertEquals(source.x()+corner[0],inverse.x());assertEquals(source.y()+corner[1],inverse.y());
        }
    }
    @Test void flowerPreservesAllCellsAndProcessingControlsWithoutPlayerInventory() {
        var slots=List.of(new DesktopNativeView.Slot(0,new DesktopWindowBounds(46,79,18,18),false,true),
            new DesktopNativeView.Slot(1,new DesktopWindowBounds(91,27,18,18),false,true),
            new DesktopNativeView.Slot(2,new DesktopWindowBounds(135,27,18,18),false,true));
        var layout=BumblezoneWindows.layout(BumblezoneWindows.Kind.FLOWER,view(slots),102);
        for(var s:slots)complete(layout,s.bounds());
        complete(layout,new DesktopWindowBounds(8,13,11,89)); // XP tower/base
        complete(layout,new DesktopWindowBounds(11,99,54,5)); // complete XP bar, including bottom border
        complete(layout,new DesktopWindowBounds(46,13,18,62)); // feed controls
        complete(layout,new DesktopWindowBounds(162,38,8,63)); // sorting and scrollbar
        assertTrue(layout.placements(new DesktopWindowBounds(7,114,162,54)).isEmpty());
    }
    @Test void everyStrictChestSizeRetainsCompleteNativeFramesAndNoPlayerCells() {
        for(int rows=1;rows<=6;rows++) {
            var slots=new ArrayList<DesktopNativeView.Slot>();
            for(int i=0;i<rows*9;i++)slots.add(new DesktopNativeView.Slot(i,new DesktopWindowBounds(7+i%9*18,17+i/9*18,18,18),false,true));
            slots.add(new DesktopNativeView.Slot(rows*9,new DesktopWindowBounds(7,rows*18+31,18,18),true,true));
            var layout=BumblezoneWindows.layout(BumblezoneWindows.Kind.COCOON,view(slots),72);
            for(var s:slots)if(!s.player())complete(layout,s.bounds());else assertTrue(layout.placements(s.bounds()).isEmpty());
        }
    }
    @Test void briefcaseKeepsEntireShapedCaseAndBothBeeRowsIncludingInverseInput() {
        var layout=BumblezoneWindows.layout(BumblezoneWindows.Kind.BRIEFCASE,view(List.of()),100);
        complete(layout,new DesktopWindowBounds(-71,-45,382,245));
        complete(layout,new DesktopWindowBounds(-11,46,22,44));
        complete(layout,new DesktopWindowBounds(217,100,22,44));
        assertTrue(layout.placements(new DesktopWindowBounds(-9999,-9999,18,18)).isEmpty());
    }
}
