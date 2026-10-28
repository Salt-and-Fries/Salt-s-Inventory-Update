package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeBatchLayoutsTest {
    private static DesktopNativeView view(int[][] positions) {
        var slots=new ArrayList<DesktopNativeView.Slot>();
        for(var p:positions)slots.add(new DesktopNativeView.Slot(slots.size(),new DesktopWindowBounds(p[0]-1,p[1]-1,18,18),false,true));
        slots.add(new DesktopNativeView.Slot(slots.size(),new DesktopWindowBounds(7,83,18,18),true,true));
        return new DesktopNativeView("native-1.20.1",new DesktopWindowBounds(0,0,176,166),new DesktopWindowBounds(7,6,162,68),slots,List.of());
    }
    private static void roundTrip(DesktopNativeView view,Function<DesktopNativeView,DesktopNativeLayout> factory) {
        var layout=factory.apply(view);
        for(var slot:view.slots()) {
            var placements=layout.placements(slot.bounds());
            if(slot.player()){assertTrue(placements.isEmpty(),"duplicate lower inventory is removed");continue;}
            if(!slot.active())continue;
            assertEquals(1,placements.size(),"each original active cell has one whole placement: "+slot.menuSlotId());
            var b=placements.get(0).bounds();var point=layout.sourceAt(b.x()+9,b.y()+9).orElseThrow();
            assertEquals(slot.bounds().x()+9,point.x());assertEquals(slot.bounds().y()+9,point.y());
        }
    }
    @Test void allAuditedMachineSlotsKeepTheirIdentityAndInversePointerMapping() {
        roundTrip(view(new int[][]{{20,33},{143,33}}),NativeBatchLayouts::ossuary);
        roundTrip(view(new int[][]{{45,17},{67,17},{56,53},{129,35}}),NativeBatchLayouts::smelter);
        roundTrip(view(new int[][]{{57,17},{80,17},{103,17},{80,53}}),NativeBatchLayouts::combiner);
        roundTrip(view(new int[][]{{27,47},{76,47},{134,47}}),NativeBatchLayouts::runes);
        roundTrip(view(new int[][]{{30,17},{30,35},{30,53},{73,35},{128,35}}),NativeBatchLayouts::fletching);
        roundTrip(view(new int[][]{{116,31},{80,53},{98,53},{116,53},{134,53},{152,53}}),NativeBatchLayouts::chunkLoader);
        roundTrip(view(new int[][]{{27,47},{76,47},{134,47}}),NativeBatchLayouts::anvil);
        roundTrip(view(new int[][]{{45,47},{80,34},{115,47}}),NativeBatchLayouts::reforge);
        roundTrip(view(new int[][]{{8,48},{26,48},{44,48},{98,48}}),NativeBatchLayouts::smithing);
        roundTrip(view(new int[][]{{80,19},{106,19},{35,43},{53,43},{71,43},{89,43},{107,43},{125,43}}),NativeBatchLayouts::linking);
        roundTrip(view(new int[][]{{49,19},{49,40},{129,34}}),NativeBatchLayouts::grindstone);
        roundTrip(view(new int[][]{{49,40},{49,19},{129,34}}),NativeBatchLayouts::unlinking);
    }
    @Test void drawerPreviewsAndCreativeOnlyUtilityDoNotInventInteractiveCells() {
        for(boolean creative:new boolean[]{false,true})for(int count:new int[]{6,8,12}) {
            int x=creative?53:62;
            var raw=new ArrayList<int[]>();for(int i=0;i<4;i++)raw.add(new int[]{x+i*18,54});
            if(count==6){raw.add(new int[]{89,20});raw.add(new int[]{71,20});}
            else {raw.add(new int[]{69,count==8?20:9});raw.add(new int[]{51,count==8?20:9});raw.add(new int[]{91,count==8?20:9});raw.add(new int[]{109,count==8?20:9});
                if(count==12){raw.add(new int[]{69,31});raw.add(new int[]{51,31});raw.add(new int[]{91,31});raw.add(new int[]{109,31});}}
            var v=view(raw.toArray(int[][]::new));
            var slots=v.slots().stream().map(s->new DesktopNativeView.Slot(s.menuSlotId(),s.bounds(),s.player(),
                s.player()||s.menuSlotId()<4?(s.menuSlotId()!=3||creative):s.menuSlotId()%2!=0)).toList();
            roundTrip(new DesktopNativeView(v.screenType(),v.imageBounds(),v.automaticBounds(),slots,v.widgets()),NativeBatchLayouts::drawers);
        }
    }
    @Test void tabArtworkAndInputRemainExternalAndDoNotIncreaseBodyHeight() {
        var l=NativeBatchLayouts.reforge(view(new int[][]{{45,47},{80,34},{115,47}}));
        assertEquals(41,l.height());var tabs=l.regions().stream().filter(DesktopNativeRegion::exterior).findFirst().orElseThrow();
        assertEquals(0,tabs.x(),"first tab shares the window's left edge");
        assertEquals(tabs.source().y(),tabs.y(),"no extra pixel of vertical offset");
        assertEquals(1,l.placements(new DesktopWindowBounds(0,0,2,4)).size(),"inactive tab continues behind the rounded frame corner");
        var point=l.exteriorSourceAt(tabs.x()+30,tabs.y()+10).orElseThrow();
        assertEquals(30,point.x());assertEquals(-14,point.y());
    }
    @Test void changedUpstreamTopologyFallsBackInsteadOfClippingAnUnknownSlot() {
        var v=view(new int[][]{{20,130},{143,33}});
        assertEquals(v.automaticBounds(),NativeBatchLayouts.ossuary(v).regions().get(0).source());
    }
    @Test void movedAnvilNameArtworkCannotSendAnUnscopedNativeRename() {
        var layout=NativeBatchLayouts.anvil(view(new int[][]{{27,47},{76,47},{134,47}}));
        var field=layout.placements(new DesktopWindowBounds(62,24,103,12)).get(0);
        assertFalse(field.interactive(),"the API-owned EditBox owns typing and authenticated rename");
        assertTrue(layout.sourceAt(field.bounds().x()+4,field.bounds().y()+4).isEmpty());
    }
    @Test void gearArtworkIncludesTheGrindstoneLeftSupportAndWholeSmithingPreview() {
        var unlink=NativeBatchLayouts.unlinking(view(new int[][]{{49,40},{49,19},{129,34}}));
        // This decoration is outside all slot cells, so slot preservation alone missed it.
        assertEquals(1,unlink.placements(new DesktopWindowBounds(30,28,11,43)).size());
        for(var layout:List.of(unlink,NativeBatchLayouts.grindstone(view(new int[][]{{49,19},{49,40},{129,34}}))))
            assertEquals(1,layout.placements(new DesktopWindowBounds(40,15,34,1)).size(),"retain the grindstone's top outline row");
        var smith=NativeBatchLayouts.smithing(view(new int[][]{{8,48},{26,48},{44,48},{98,48}}));
        assertEquals(1,smith.placements(new DesktopWindowBounds(124,24,37,53)).size(),"head, left arm and feet remain captured");
        assertEquals(1,smith.placements(new DesktopWindowBounds(117,20,56,60)).size(),"equipped-result preview keeps its margin");
        assertTrue(smith.placements(new DesktopWindowBounds(173,30,1,20)).isEmpty(),"omit the native container's right bevel");
    }
}
