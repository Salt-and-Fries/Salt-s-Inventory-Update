package com.salts_inventory_update.compat.aether;
import com.salts_inventory_update.api.client.desktop.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AetherLayoutsTest {
    private void complete(DesktopNativeLayout l,int x,int y,int w,int h){
        var p=l.placements(new DesktopWindowBounds(x,y,w,h));assertEquals(1,p.size());var t=p.get(0).bounds();assertEquals(w,t.width());assertEquals(h,t.height());
        for(int[] c:new int[][]{{0,0},{w-1,0},{0,h-1},{w-1,h-1}}){var s=l.sourceAt(t.x()+c[0],t.y()+c[1]).orElseThrow();assertEquals(x+c[0],s.x());assertEquals(y+c[1],s.y());}
    }
    @Test void altarAndFreezerKeepCompleteResultBordersAndRecipeButtons(){
        for(var k:new AetherWindows.Kind[]{AetherWindows.Kind.ALTAR,AetherWindows.Kind.FREEZER}){
            var l=AetherWindows.layout(k,40);complete(l,55,16,18,18);complete(l,55,52,18,18);complete(l,110,30,26,26);complete(l,20,35,20,18);
            assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
        }
    }
    @Test void incubatorRetainsBothCellsAndTheFullHeightMeter(){
        var l=AetherWindows.layout(AetherWindows.Kind.INCUBATOR,53);complete(l,72,16,18,18);complete(l,72,52,18,18);complete(l,103,16,9,54);complete(l,37,35,20,18);
        assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
    }
    @Test void loreRetainsBothPagesSlotAndRelocatedNavigationButNoInventory(){
        var l=AetherWindows.layout(AetherWindows.Kind.LORE,65);complete(l,12,2,234,105);complete(l,82,62,18,18);complete(l,14,169,20,20);complete(l,221,169,20,20);
        assertTrue(l.placements(new DesktopWindowBounds(47,112,162,76)).isEmpty());
    }
    @Test void translatedTitlesWidenTheSurfaceWithoutChangingNativeWorkingGeometry(){
        for(var k:AetherWindows.Kind.values()){var l=AetherWindows.layout(k,320);complete(l,k.body.x(),k.body.y(),k.body.width(),k.body.height());assertTrue(l.width()>=352);}
    }
}
