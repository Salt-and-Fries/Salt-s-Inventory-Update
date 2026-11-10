package com.salts_inventory_update.compat.zenith;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.zenith.ZenithWindows.Kind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZenithLayoutsTest {
    private void complete(DesktopNativeLayout layout,int x,int y,int w,int h){
        var source=new DesktopWindowBounds(x,y,w,h);
        var placements=layout.placements(source);assertEquals(1,placements.size(),source.toString());
        var target=placements.get(0).bounds();
        for(int[] p:new int[][]{{0,0},{w-1,0},{0,h-1},{w-1,h-1}}){
            var back=layout.sourceAt(target.x()+p[0],target.y()+p[1]).orElseThrow();
            assertEquals(x+p[0],back.x());assertEquals(y+p[1],back.y());
        }
    }
    @Test void allEngravedPanelsRetainTheirClosingRimsAndExcludePlayerInventory(){
        for(var kind:Kind.values())if(kind.bodyHeight>0){
            var l=ZenithClient.layout(kind,90,false);complete(l,0,0,176,kind.bodyHeight);
            assertEquals(kind.bodyHeight,l.height());assertFalse(l.scrollable());
            assertTrue(l.placements(new DesktopWindowBounds(7,kind.bodyHeight+3,162,76)).isEmpty());
        }
    }
    @Test void salvagingAndGemCuttingKeepEveryNativeSlotAndItsSurroundingArtwork(){
        var salvage=ZenithClient.layout(Kind.SALVAGING,90,false);
        for(int y:new int[]{17,36,55})for(int x:new int[]{17,36,55,74,124,143})complete(salvage,x-1,y-1,18,18);
        complete(salvage,98,34,18,18);
        var gems=ZenithClient.layout(Kind.GEM_CUTTING,90,false);
        for(int[] p:new int[][]{{62,45},{90,64},{33,64},{62,12}})complete(gems,p[0]-1,p[1]-1,18,18);
        complete(gems,8,8,160,77);
    }
    @Test void reforgingKeepsAllThreeCandidateCirclesAndTheirCostDots(){
        var l=ZenithClient.layout(Kind.REFORGING,90,false);
        for(int[] p:new int[][]{{81,62},{39,40},{123,86},{27,135},{81,135},{135,135}})complete(l,p[0]-1,p[1]-1,18,18);
        complete(l,20,129,138,35);
    }
    @Test void librariesAndAugmentingRetainSearchDropdownAndActions(){
        var lib=ZenithClient.layout(Kind.LIBRARY,130,false);
        for(int y:new int[]{18,77,106})complete(lib,141,y-1,18,18);
        complete(lib,13,16,120,115);
        var aug=ZenithClient.layout(Kind.AUGMENTING,90,false);
        complete(aug,39,17,123,84);complete(aug,60,111,29,13);complete(aug,112,111,29,13);
    }
    @Test void fletchingRetainsTheFullArrowTailAndOversizeResultBorder(){
        var l=ZenithClient.layout(Kind.FLETCHING,80,false);
        complete(l,6,23,39,40);complete(l,119,30,26,26);
        for(int y:new int[]{17,35,53})complete(l,47,y-1,18,18);
        assertTrue(l.placements(new DesktopWindowBounds(7,83,162,76)).isEmpty());
    }
    @Test void enchantingRetainsThreeOffersAndAllStatsWithAnExteriorInformationTab(){
        var l=ZenithClient.layout(Kind.ENCHANTING,85,true);
        complete(l,14,46,18,18);complete(l,34,46,18,18);
        complete(l,60,14,108,57);complete(l,7,74,163,28);
        var p=l.exteriorSourceAt(150,-8).orElseThrow();assertEquals(150,p.x());assertEquals(-8,p.y());
        assertTrue(ZenithClient.layout(Kind.ENCHANTING,85,false).exteriorSourceAt(150,-8).isEmpty());
        assertTrue(l.placements(new DesktopWindowBounds(7,114,162,76)).isEmpty());
    }
}
