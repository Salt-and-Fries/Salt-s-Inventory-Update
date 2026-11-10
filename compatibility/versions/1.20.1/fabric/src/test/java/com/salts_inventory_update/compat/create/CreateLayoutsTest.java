package com.salts_inventory_update.compat.create;

import com.google.gson.*;
import com.salts_inventory_update.api.client.desktop.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateLayoutsTest {
    @Test void nativeControlsRetainCompleteInteractiveDestinations() throws Exception {
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/create-native-fidelity-controls.json"),StandardCharsets.UTF_8)){
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()){
                var row=raw.getAsJsonObject();var widgets=new ArrayList<DesktopNativeView.Widget>();
                for(var entry:row.getAsJsonArray("widgets")){
                    var a=entry.getAsJsonObject();widgets.add(new DesktopNativeView.Widget(a.get("id").getAsString(),"native-reference",net.minecraft.network.chat.Component.empty(),
                        new DesktopWindowBounds(a.get("x").getAsInt(),a.get("y").getAsInt(),a.get("width").getAsInt(),a.get("height").getAsInt()),true,true));
                }
                var bounds=new DesktopWindowBounds(0,0,row.get("width").getAsInt(),row.get("height").getAsInt());
                var view=new DesktopNativeView(row.get("screen").getAsString(),bounds,bounds,List.of(),widgets);
                var layout=CreateLayouts.build(CreateWindows.Kind.valueOf(row.get("kind").getAsString()),view,null,new CreateWindows.State());
                for(var widget:widgets){
                    var places=layout.placements(widget.bounds()).stream().filter(DesktopNativeLayout.Placement::interactive).toList();
                    assertEquals(1,places.size(),row.get("id")+" / "+widget.id());
                    var place=places.get(0);var target=place.bounds();
                    var point=(place.exterior()?layout.exteriorSourceAt(target.x()+target.width()/2d,target.y()+target.height()/2d)
                        :layout.sourceAt(target.x()+target.width()/2d,target.y()+target.height()/2d)).orElseThrow();
                    assertEquals(widget.bounds().x()+widget.bounds().width()/2d,point.x());
                    assertEquals(widget.bounds().y()+widget.bounds().height()/2d,point.y());
                }
                assertTrue(layout.regions().stream().filter(r->r.id().equals("native-companion")).noneMatch(DesktopNativeRegion::interactive));
            }
        }
    }
    @Test void completeVisibleCellsHaveExactlyOneInvertibleDestination() throws Exception {
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/create-native-reference-geometry.json"),StandardCharsets.UTF_8)) {
            for(var raw:JsonParser.parseReader(reader).getAsJsonArray()) {
                var r=raw.getAsJsonObject();String id=r.get("id").getAsString(),kind=r.get("kind").getAsString();
                if(kind.equals("category-editor")||kind.equals("schedule-editor"))continue;
                var slots=new ArrayList<DesktopNativeView.Slot>();
                for(var item:r.getAsJsonArray("slots")) {
                    var a=item.getAsJsonObject();int x=a.get("x").getAsInt(),y=a.get("y").getAsInt();
                    boolean player=a.get("player").getAsBoolean();
                    slots.add(new DesktopNativeView.Slot(a.get("index").getAsInt(),new DesktopWindowBounds(x-1,y-1,18,18),player,x>=0&&y>=0));
                }
                var size=r.getAsJsonObject("bounds");var image=new DesktopWindowBounds(0,0,size.get("width").getAsInt(),size.get("height").getAsInt());
                var v=new DesktopNativeView(r.get("screen").getAsString(),image,image,slots,List.of());
                var state=new CreateWindows.State();
                var k=switch(kind){case "toolbox"->CreateWindows.Kind.TOOLBOX;case "schematic-table"->CreateWindows.Kind.SCHEMATIC_TABLE;case "blueprint"->CreateWindows.Kind.BLUEPRINT;case "category"->CreateWindows.Kind.CATEGORIES;case "package"->CreateWindows.Kind.PACKAGE_FILTER;case "fuze"->CreateWindows.Kind.TIMED_FUZE;default->CreateWindows.Kind.valueOf(kind.toUpperCase(Locale.ROOT));};
                var layout=CreateLayouts.build(k,v,null,state);
                for(var slot:slots) {
                    if(slot.player()||!slot.active())continue;
                    var places=layout.placements(slot.bounds());assertEquals(1,places.size(),id+" slot "+slot.menuSlotId());
                    var at=places.get(0).bounds();
                    for(int[] point:new int[][]{{1,1},{9,9},{17,17}}) {
                        var source=layout.sourceAt(at.x()+point[0],at.y()+point[1]).orElseThrow();
                        assertEquals(slot.bounds().x()+point[0],source.x(),id);assertEquals(slot.bounds().y()+point[1],source.y(),id);
                    }
                    for(var region:layout.regions())if(!region.covers(slot.bounds()))
                        assertFalse(region.pieces().stream().anyMatch(piece->DesktopNativeRegion.intersects(piece,slot.bounds())),id+" partially clipped cell "+slot.menuSlotId()+" in "+region.id());
                }
            }
        }
    }
}
