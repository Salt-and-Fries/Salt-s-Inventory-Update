package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeMachineLayoutsTest {
    private static DesktopWindowBounds box(int x,int y,int width,int height){return new DesktopWindowBounds(x,y,width,height);}
    private static DesktopNativeView view(int[][] cells,List<DesktopNativeView.Widget> widgets){
        var slots=new ArrayList<DesktopNativeView.Slot>();
        for(var xy:cells)slots.add(new DesktopNativeView.Slot(slots.size(),box(xy[0]-1,xy[1]-1,18,18),false,true));
        slots.add(new DesktopNativeView.Slot(slots.size(),box(7,107,18,18),true,true));
        return new DesktopNativeView("audited-26.2",box(0,0,176,189),box(7,10,162,84),slots,widgets);
    }
    private static void realSlots(DesktopNativeView view,Function<DesktopNativeView,DesktopNativeLayout> factory){
        var layout=factory.apply(view);
        assertEquals(0,layout.padding(),"Salt's frame already supplies padding");
        for(var slot:view.slots()){
            var placements=layout.placements(slot.bounds());
            if(slot.player()){assertTrue(placements.isEmpty(),"native lower inventory is excluded");continue;}
            assertEquals(1,placements.size(),"each real cell has exactly one complete placement");
            var p=placements.get(0);var b=p.bounds();
            var source=(p.exterior()?layout.exteriorSourceAt(b.x()+9,b.y()+9):layout.sourceAt(b.x()+9,b.y()+9)).orElseThrow();
            assertEquals(slot.bounds().x()+9,source.x());assertEquals(slot.bounds().y()+9,source.y());
        }
    }
    @Test void allWorkbenchRolesRetainTheirWholeCellAndInverseInput(){
        realSlots(view(new int[][]{{30,17},{48,17},{66,17},{30,35},{48,35},{66,35},{124,26},{92,55},{124,55}},List.of()),NativeMachineLayouts::cooking);
        realSlots(view(new int[][]{{40,18},{58,18},{40,36},{58,36},{115,28},{83,56},{115,56}},List.of()),NativeMachineLayouts::copperPot);
        realSlots(view(new int[][]{{80,20},{26,48},{134,48}},List.of()),NativeMachineLayouts::identifying);
        realSlots(view(new int[][]{{80,22}},List.of()),NativeMachineLayouts::fossil);
        realSlots(view(new int[][]{{26,54},{80,54},{134,54},{80,14}},List.of()),NativeMachineLayouts::imbuing);
        realSlots(view(new int[][]{{44,29},{44,50},{80,50},{116,50},{79,21}},List.of()),NativeMachineLayouts::combustion);
        for(var cells:List.of(new int[][]{{21,33},{143,33}},new int[][]{{17,33},{146,33}}))
            realSlots(view(cells,List.of()),NativeMachineLayouts::sawmill);
    }
    @Test void recipeToggleFitsInsideACompactExtraColumn(){
        var button=new DesktopNativeView.Widget("book","ImageButton",Component.empty(),box(5,34,20,18),true,true);
        var layout=NativeMachineLayouts.cooking(view(new int[][]{{30,17},{124,55}},List.of(button)));
        var placed=layout.placements(button.bounds()).get(0);
        assertFalse(placed.exterior());assertEquals(0,placed.bounds().x());
        assertEquals(139,layout.width());assertEquals(57,layout.height());
        assertEquals(5,layout.sourceAt(placed.bounds().x(),placed.bounds().y()).orElseThrow().x());
        assertTrue(layout.regions().stream().noneMatch(DesktopNativeRegion::exterior));
        realSlots(view(new int[][]{{30,17},{124,55}},List.of(button)),NativeMachineLayouts::cooking);
        var copper=NativeMachineLayouts.copperPot(view(new int[][]{{40,18},{115,56}},List.of(button)));
        assertEquals(122,copper.width());assertEquals(57,copper.height());
    }
    @Test void fossilFindsItsSocketAfterTheForgePlayerInventory(){
        var original=view(new int[][]{{80,22}},List.of());
        var slots=List.of(new DesktopNativeView.Slot(0,box(7,85,18,18),true,true),
                new DesktopNativeView.Slot(36,box(79,21,18,18),false,true));
        var forge=new DesktopNativeView(original.screenType(),original.imageBounds(),original.automaticBounds(),slots,List.of());
        realSlots(forge,NativeMachineLayouts::fossil);
        assertEquals(box(72,14,32,32),NativeMachineLayouts.fossil(forge).regions().get(0).source());
    }
    @Test void particleToggleReusesTheNativeControlWithoutADeadTitleBand(){
        var layout=NativeMachineLayouts.conditioning(view(new int[][]{{26,17},{62,53},{98,35},{134,35}},List.of()));
        var point=layout.sourceAt(125,3).orElseThrow();
        assertEquals(166,point.x());assertEquals(9,point.y());
        assertEquals(55,layout.height());assertTrue(layout.regions().stream().noneMatch(DesktopNativeRegion::exterior));
    }
    @Test void forgeReservoirStaysOutsideWhileNativeLabelsDoNotCoverItsSlots(){
        var input=new ArrayList<int[]>();input.add(new int[]{8,74});input.add(new int[]{145,50});
        for(int row=0;row<2;row++)for(int col=0;col<5;col++)input.add(new int[]{44+18*col,43+18*row});
        var view=view(input.toArray(int[][]::new),List.of());realSlots(view,NativeMachineLayouts::alloyForge);
        var layout=NativeMachineLayouts.alloyForge(view);
        assertEquals(71,layout.height());assertTrue(layout.regions().stream().anyMatch(r->r.exterior()&&r.y()<0));
    }
    @Test void unexpectedSlotTopologyFallsBackRatherThanLosingARealSlot(){
        var view=view(new int[][]{{10,20},{134,48}},List.of());
        var layout=NativeMachineLayouts.identifying(view);
        assertEquals(view.automaticBounds(),layout.regions().get(0).source());
    }
    @Test void standaloneAlloyedKeepsAllElevenMachineCells() {
        var cells=new ArrayList<int[]>();
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)cells.add(new int[]{30+18*col,17+18*row});
        cells.add(new int[]{93,53});cells.add(new int[]{124,18});
        var v=view(cells.toArray(int[][]::new),List.of());
        realSlots(v,NativeMachineLayouts::alloyed);
        assertEquals(box(29,14,116,57),NativeMachineLayouts.alloyed(v).regions().get(0).source());
    }
}
