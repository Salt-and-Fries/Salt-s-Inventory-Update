package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeTemplateLayoutTest {
    private DesktopWindowBounds rect(int x,int y,int w,int h){return new DesktopWindowBounds(x,y,w,h);}
    @Test void movedChunkUsesTheSameForwardAndInverseGeometryForSlotsAndInput(){
        var slot=rect(56,17,18,18);
        var layout=DesktopNativeLayout.builder().inherit(rect(7,16,162,60)).padding(4).move("input",slot,170,3).build();
        assertTrue(layout.sourceAt(4+56-7+8,4+17-16+8).isEmpty());
        var placement=layout.placements(slot).get(0);assertEquals(rect(174,7,18,18),placement.bounds());
        var hit=layout.sourceAt(placement.bounds().x()+9,placement.bounds().y()+9).orElseThrow();
        assertEquals("input",hit.regionId());assertEquals(65,hit.x());assertEquals(26,hit.y());
    }
    @Test void subtractionPreservesAreaWithoutOverlappingOrPaintingTheHole(){
        var region=new DesktopNativeRegion("body",rect(0,0,100,80),0,0).cut(rect(20,10,40,30));
        var pieces=region.pieces();assertEquals(100*80-40*30,pieces.stream().mapToInt(b->b.width()*b.height()).sum());
        for(int i=0;i<pieces.size();i++)for(int j=i+1;j<pieces.size();j++)assertFalse(DesktopNativeRegion.intersects(pieces.get(i),pieces.get(j)));
        for(var p:pieces)assertFalse(DesktopNativeRegion.intersects(p,rect(20,10,40,30)));
        assertFalse(region.contains(30,20));assertTrue(region.contains(10,20));
    }
    @Test void laterRegionsOwnOverlappingInputWhileRealSlotIdsRemainExternal(){
        var layout=DesktopNativeLayout.builder().keep("first",rect(0,0,20,20),0,0).keep("second",rect(100,100,20,20),0,0).build();
        var point=layout.sourceAt(8,9).orElseThrow();assertEquals("second",point.regionId());assertEquals(108,point.x());assertEquals(109,point.y());
    }
    @Test void visualOnlyPiecesDoNotInterceptUnderlyingInteractiveContent(){
        var layout=DesktopNativeLayout.builder().keep("body",rect(0,0,20,20),0,0)
            .region(new DesktopNativeRegion("mirror",rect(100,100,20,20),0,0,false,List.of())).build();
        assertEquals("body",layout.sourceAt(5,5).orElseThrow().regionId());
        assertEquals(1,layout.placements(rect(101,101,5,5)).size());
    }
    @Test void partialCellsAreNotOfferedAsWholeInteractiveSlotPlacements(){
        var slot=rect(8,18,18,18);var layout=DesktopNativeLayout.builder().keep("partial",rect(8,18,9,18),0,0).build();
        assertTrue(layout.placements(slot).isEmpty());
        assertFalse(new DesktopNativeRegion("hole",rect(0,0,100,100),0,0).cut(rect(9,19,2,2)).covers(slot));
    }
    @Test void profileRoundTripRetainsCutoutsLayerOrderPaddingAndViewportPolicy(){
        var layout=DesktopNativeLayout.builder().inherit(rect(-5,-20,170,100)).padding(6).canvas(200,140).move("control",rect(10,10,20,20),180,30).scrollable(true).build();
        assertEquals(layout,DesktopNativeProfiles.decode(DesktopNativeProfiles.encode(layout)));
        assertEquals(212,layout.width());assertEquals(140,layout.height());
    }
    @Test void invalidDimensionsDuplicatesAndExcessiveShapesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->DesktopNativeLayout.builder().keep("x",rect(0,0,10,10),-1,0));
        assertThrows(IllegalArgumentException.class,()->DesktopNativeLayout.builder().keep("x",rect(0,0,10,10),0,0).keep("x",rect(0,0,10,10),20,0).build());
        assertThrows(IllegalArgumentException.class,()->DesktopNativeLayout.builder().keep("x",rect(0,0,5000,10),0,0).build());
        assertThrows(IllegalArgumentException.class,()->DesktopNativeProfiles.decode(new com.google.gson.JsonObject()));
    }
    @Test void nativeSlotAnchorsUseObservedGeometryAndCanFollowLayoutChanges(){
        var old=new DesktopNativeView("screen",rect(0,0,176,166),rect(7,16,162,56),List.of(new DesktopNativeView.Slot(2,rect(115,34,18,18),false,true)),List.of());
        var next=new DesktopNativeView("screen",rect(0,0,230,166),rect(7,16,216,56),List.of(new DesktopNativeView.Slot(2,rect(169,34,18,18),false,true)),List.of());
        assertEquals(115,old.slotBounds(2).orElseThrow().x());assertEquals(169,next.slotBounds(2).orElseThrow().x());assertTrue(next.slotBounds(99).isEmpty());
    }
    @Test void exteriorFragmentsDoNotChangeBodySizeOrItsSlotMapping(){
        for(int height:new int[]{8,12,25,45}){
            var layout=DesktopNativeLayout.builder().keep("body",rect(31,14,123,56),0,0)
                .exterior("preview",rect(74,-height,92,height),23,-height).build();
            assertEquals(123,layout.width());assertEquals(56,layout.height());
            assertTrue(layout.sourceAt(26,-1).isEmpty());
            var p=layout.exteriorSourceAt(28,-height+2).orElseThrow();
            assertEquals(79,p.x());assertEquals(-height+2,p.y());
            assertTrue(layout.exteriorSourceAt(22,-height+2).isEmpty());
            assertEquals(layout,DesktopNativeProfiles.decode(DesktopNativeProfiles.encode(layout)));
        }
    }
    @Test void exteriorSlotPlacementsKeepWindowLocalCoordinatesAndCutouts(){
        var cell=rect(90,-30,18,18);
        var layout=DesktopNativeLayout.builder().keep("body",rect(0,0,100,50),0,0)
            .exterior("panel",rect(74,-45,92,45),12,-45).build();
        var p=layout.placements(cell).get(0);
        assertTrue(p.exterior());assertEquals(rect(28,-30,18,18),p.bounds());
        var hole=layout.toBuilder().hide(cell).build();
        assertTrue(hole.placements(cell).isEmpty());assertTrue(hole.exteriorSourceAt(30,-25).isEmpty());
    }
}
