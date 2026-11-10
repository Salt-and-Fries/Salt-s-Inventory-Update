package com.salts_inventory_update.compat.enchanted;

import com.salts_inventory_update.api.client.desktop.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnchantedLayoutsTest {
    private void complete(DesktopNativeLayout layout,int x,int y,int w,int h) {
        var source=new DesktopWindowBounds(x,y,w,h);
        var placements=layout.placements(source);
        assertEquals(1,placements.size(),source.toString());
        var target=placements.get(0).bounds();
        assertEquals(w,target.width());assertEquals(h,target.height());
        for(int[] corner:new int[][]{{0,0},{w-1,0},{0,h-1},{w-1,h-1}}) {
            var inverse=layout.sourceAt(target.x()+corner[0],target.y()+corner[1]).orElseThrow();
            assertEquals(x+corner[0],inverse.x());assertEquals(y+corner[1],inverse.y());
        }
    }
    @Test void ovenKeepsAllFiveCellsWithJarGhostsAndTheEntireHeatAndProgressSprites() {
        var l=EnchantedWindows.layout(EnchantedWindows.Kind.OVEN,60);
        for(int[] p:new int[][]{{53,17},{53,53},{80,53},{107,17},{107,53}})complete(l,p[0]-1,p[1]-1,18,18);
        complete(l,77,35,14,15);complete(l,77,17,24,17);
        assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
    }
    @Test void wheelKeepsTheTriangleAndCompleteAnimatedMechanism() {
        var l=EnchantedWindows.layout(EnchantedWindows.Kind.WHEEL,71);
        for(int[] p:new int[][]{{45,23},{33,47},{57,47},{130,35}})complete(l,p[0]-1,p[1]-1,18,18);
        complete(l,27,20,98,45);
        assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
    }
    @Test void distilleryKeepsAllSevenBordersAndBranchesButExcludesTheOldTitle() {
        var l=EnchantedWindows.layout(EnchantedWindows.Kind.DISTILLERY,44);
        for(int[] p:new int[][]{{32,35},{54,25},{54,45},{127,7},{127,26},{127,45},{127,64}})complete(l,p[0]-1,p[1]-1,18,18);
        complete(l,69,15,56,59);complete(l,93,27,12,29);
        assertTrue(l.placements(new DesktopWindowBounds(66,6,44,9)).isEmpty());
        assertTrue(l.placements(new DesktopWindowBounds(29,72,29,9)).isEmpty());
        assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
    }
    @Test void poppetShelfKeepsFourSeparatedCellsWithoutPlayerInventory() {
        var l=EnchantedWindows.layout(EnchantedWindows.Kind.SHELF,64);
        for(int x:new int[]{47,69,91,113})complete(l,x-1,17,18,18);
        assertTrue(l.placements(new DesktopWindowBounds(7,48,162,76)).isEmpty());
    }
    @Test void altarRetainsFullReadoutAreaAndNoOtherNativeSurface() {
        var l=EnchantedWindows.layout(EnchantedWindows.Kind.ALTAR,25);
        complete(l,20,32,136,24);
        assertTrue(l.placements(new DesktopWindowBounds(0,0,176,16)).isEmpty());
    }
}
