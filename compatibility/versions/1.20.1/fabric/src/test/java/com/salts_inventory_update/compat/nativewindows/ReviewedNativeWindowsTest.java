package com.salts_inventory_update.compat.nativewindows;

import com.google.gson.*;
import com.salts_inventory_update.api.client.desktop.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewedNativeWindowsTest {
    @Test void wandToolPickerAcceptsBothPublisherMenuIdsAndRetainsItsSelectionCells() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var profile=Arrays.stream(profiles).filter(p->p.id.equals("design-wands-tools-request")).findFirst().orElseThrow();
        assertTrue(ReviewedNativeWindows.accepts(profile,new net.minecraft.resources.ResourceLocation("wands","wand_menu")));
        assertTrue(ReviewedNativeWindows.accepts(profile,new net.minecraft.resources.ResourceLocation("wands","wand_tools_menu")));
        assertFalse(ReviewedNativeWindows.accepts(profile,new net.minecraft.resources.ResourceLocation("wands","palette_menu")));
        assertTrue(profile.playerSlots,"The picker must keep the inventory cells used to select tool sources");
    }
    @Test void completeAnvilEditBoxStaysInsideItsNativeFrameAndGuideIsRetained() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var profile=Arrays.stream(profiles).filter(p->p.id.equals("minecraft-anvil")).findFirst().orElseThrow();
        var field=new DesktopWindowBounds(62,24,103,12);
        var widget=new DesktopNativeView.Widget("name","EditBox",Component.empty(),field,true,true);
        var view=new DesktopNativeView("fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen",
            new DesktopWindowBounds(0,0,176,166),new DesktopWindowBounds(7,7,162,68),List.of(),List.of(widget));
        var layout=ReviewedNativeWindows.compose(profile,view);
        var at=layout.placements(field).get(0).bounds();
        assertEquals(3,at.x(),"Name input stays in its existing border instead of a second column");
        assertEquals(5,at.y());
        assertTrue(layout.regions().stream().noneMatch(r->r.id().startsWith("widget/")));
        assertEquals(1,layout.placements(new DesktopWindowBounds(155,6,15,12)).size(),"Render-only formatting guide remains reachable");
        var bottom=layout.sourceAt(at.x()+2,at.y()+field.height()-1).orElseThrow();
        assertEquals(field.y()+field.height()-1,bottom.y(),"Bottom row has an inverse mouse mapping");
    }
    @Test void profilesClaimOnlyExplicitMenuIdsRatherThanEveryScreenInTheMod() {
        var p=new ReviewedNativeWindows.Profile();p.mods=new String[]{"waystones"};p.menu="waystones:warp_plate";
        p.menus=new String[]{"waystones:warp_plate"};
        assertTrue(ReviewedNativeWindows.accepts(p,new net.minecraft.resources.ResourceLocation("waystones","warp_plate")));
        assertFalse(ReviewedNativeWindows.accepts(p,new net.minecraft.resources.ResourceLocation("waystones","waystone_selection")));
        assertFalse(ReviewedNativeWindows.accepts(p,new net.minecraft.resources.ResourceLocation("waystones","sharestone_selection")));
    }
    private static JsonArray read(String path) {
        return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(
            ReviewedNativeWindowsTest.class.getResourceAsStream(path)),StandardCharsets.UTF_8)).getAsJsonArray();
    }
    private static int n(JsonObject o,String key) { return o.get(key).getAsInt(); }
    private static DesktopNativeView view(JsonObject data) {
        var b=data.getAsJsonObject("bounds");
        var slots=new ArrayList<DesktopNativeView.Slot>();
        for(var raw:data.getAsJsonArray("slots")) {
            var s=raw.getAsJsonObject();slots.add(new DesktopNativeView.Slot(n(s,"index"),
                new DesktopWindowBounds(n(s,"x")-1,n(s,"y")-1,18,18),s.get("player").getAsBoolean(),true));
        }
        var widgets=new ArrayList<DesktopNativeView.Widget>();
        for(var raw:data.getAsJsonArray("widgets")) {
            var w=raw.getAsJsonObject();if(n(w,"width")<=0||n(w,"height")<=0)continue;
            widgets.add(new DesktopNativeView.Widget("widget/"+widgets.size(),w.get("type").getAsString(),Component.empty(),
                new DesktopWindowBounds(n(w,"x")-n(b,"x"),n(w,"y")-n(b,"y"),n(w,"width"),n(w,"height")),true,true));
        }
        return new DesktopNativeView(data.get("screen").getAsString(),new DesktopWindowBounds(0,0,n(b,"width"),n(b,"height")),
            new DesktopWindowBounds(7,7,Math.max(1,n(b,"width")-14),Math.max(1,n(b,"height")-14)),slots,widgets);
    }
    @Test void everyReferenceCellHasOneCompletePaintAndInverseInputMapping() {
        var gson=new Gson();var profiles=gson.fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        for(var data:read("/reviewed-native-reference-geometry.json")) {
            var v=view(data.getAsJsonObject());var p=Arrays.stream(profiles).filter(a->a.screen.equals(v.screenType())).findFirst().orElseThrow();
            var layout=ReviewedNativeWindows.compose(p,v);
            assertFalse(layout.regions().stream().anyMatch(r->r.id().equals("automatic")),p.id+": reference must use its reviewed composition");
            for(var slot:v.slots())if(!slot.player()||p.playerSlots) {
                var places=layout.placements(slot.bounds());assertEquals(1,places.size(),p.id+" cell "+slot.menuSlotId());
                var place=places.get(0);var at=place.bounds();var point=(place.exterior()?layout.exteriorSourceAt(at.x()+9,at.y()+9):layout.sourceAt(at.x()+9,at.y()+9)).orElseThrow(
                    ()->new AssertionError(p.id+" cell "+slot.menuSlotId()+" has no inverse mapping at "+at+" in "+layout));
                assertEquals(slot.bounds().x()+9,point.x(),p.id);assertEquals(slot.bounds().y()+9,point.y(),p.id);
            }
        }
    }
    @Test void bookAndDestinationViewersNeverBecomeReviewedInventoryProfiles() {
        for(var raw:read("/salts_inventory_compat/native-windows/project-everything.json")) {
            var s=raw.getAsJsonObject().get("screen").getAsString();
            assertFalse(s.contains("AlbumScreen")||s.contains("WaystoneSelection")||s.contains("SharestoneSelection"),s);
        }
    }
    @Test void paletteControlsAndGradientSpinnerStayAboveTheCompleteSlotGrid() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var p=Arrays.stream(profiles).filter(a->a.id.equals("item-wands-palette")).findFirst().orElseThrow();
        var data=java.util.stream.StreamSupport.stream(read("/reviewed-native-reference-geometry.json").spliterator(),false)
            .map(JsonElement::getAsJsonObject).filter(a->a.get("screen").getAsString().equals(p.screen)).findFirst().orElseThrow();
        var v=view(data);var layout=ReviewedNativeWindows.compose(p,v);
        int firstRow=layout.placements(v.slots().get(0).bounds()).get(0).bounds().y();
        assertEquals(18,firstRow,"Reserve a control row below the window title");
        for(var bounds:List.of(new DesktopWindowBounds(50,5,10,10),new DesktopWindowBounds(66,4,54,12),new DesktopWindowBounds(120,4,30,12))) {
            var places=layout.placements(bounds);assertEquals(1,places.size());
            var at=places.get(0).bounds();assertTrue(at.y()+at.height()<firstRow);
            var source=layout.sourceAt(at.x()+1,at.y()+1).orElseThrow();
            assertEquals(bounds.x()+1,source.x());assertEquals(bounds.y()+1,source.y());
        }
    }
    @Test void decorativeArtworkOutsideTheInteractiveCellsIsNotClipped() {
        var gson=new Gson();
        var profiles=gson.fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var artwork=Map.of(
            "meadow-cooking-cauldron",List.of(new DesktopWindowBounds(92,9,16,18)),
            "meadow-fondue",List.of(new DesktopWindowBounds(35,30,40,25),new DesktopWindowBounds(40,55,18,14),new DesktopWindowBounds(116,21,25,25)),
            "meadow-woodcutter",List.of(new DesktopWindowBounds(140,30,24,24)),
            "nethervinery-crimson-fermentation-barrel",List.of(new DesktopWindowBounds(28,15,45,52)),
            "exposure-film-rename",List.of(new DesktopWindowBounds(28,17,150,16)),
            "bf-gallery-painting-selector",List.of(new DesktopWindowBounds(87,8,66,12)),
            "own-bf-minion-trade-real",List.of(new DesktopWindowBounds(129,50,26,26)),
            "spell-engine-spell-binding",List.of(new DesktopWindowBounds(59,13,110,60)));
        for(var data:read("/reviewed-native-reference-geometry.json")) {
            var v=view(data.getAsJsonObject());var p=Arrays.stream(profiles).filter(a->a.screen.equals(v.screenType())).findFirst().orElseThrow();
            if(!artwork.containsKey(p.id))continue;
            var layout=ReviewedNativeWindows.compose(p,v);
            for(var art:artwork.get(p.id))assertEquals(1,layout.placements(art).size(),p.id+" complete artwork "+art);
        }
    }
    @Test void visualWorkbenchKeepsTheGridOutputFrameAndRecipeToggle() {
        var v=view(read("/reviewed-visual-workbench-reference.json").get(0).getAsJsonObject());
        var layout=NativeBatchLayouts.visualWorkbench(v);
        assertEquals(141,layout.width());assertEquals(56,layout.height());
        for(var slot:v.slots()) {
            var places=layout.placements(slot.bounds());
            if(slot.player()){assertTrue(places.isEmpty());continue;}
            assertEquals(1,places.size());var at=places.get(0).bounds();
            var p=layout.sourceAt(at.x()+9,at.y()+9).orElseThrow();
            assertEquals(slot.bounds().x()+9,p.x());assertEquals(slot.bounds().y()+9,p.y());
        }
        assertEquals(1,layout.placements(new DesktopWindowBounds(119,30,26,26)).size());
        assertEquals(1,layout.placements(new DesktopWindowBounds(5,34,20,18)).size());
    }
    @Test void laterVineryBottleRowStaysUnderItsNativePipes() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var v=view(read("/reviewed-vinery-fermentation-1.4.41.json").get(0).getAsJsonObject());
        var p=Arrays.stream(profiles).filter(a->a.screen.equals(v.screenType())).findFirst().orElseThrow();
        var layout=ReviewedNativeWindows.compose(p,v);
        assertEquals(1,layout.regions().size(),"Keep the machine together instead of appending a bottle column");
        var first=v.slots().stream().filter(s->s.menuSlotId()==0).findFirst().orElseThrow();
        var origin=layout.placements(first.bounds()).get(0).bounds();
        for(var slot:v.slots())if(!slot.player()) {
            var places=layout.placements(slot.bounds());assertEquals(1,places.size());
            var at=places.get(0).bounds();
            assertEquals(slot.bounds().x()-first.bounds().x(),at.x()-origin.x());
            assertEquals(slot.bounds().y()-first.bounds().y(),at.y()-origin.y());
        }
        assertTrue(layout.width()<120,"No extra right column");
        assertTrue(layout.sourceAt(origin.x(),origin.y()+56).isEmpty(),"No remnant of the native Inventory label beside the bottles");
    }
    @Test void xpPanelBevelAndGravityStatusLineAreComplete() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var expected=Map.of("item-archon-experience-pouch",new DesktopWindowBounds(7,4,216,98),
            "ad-astra-giselle-addon-gravity-normalizer",new DesktopWindowBounds(104,104,68,9));
        var seen=new HashSet<String>();
        for(var raw:read("/reviewed-native-reference-geometry.json")) {
            var v=view(raw.getAsJsonObject());var p=Arrays.stream(profiles).filter(a->a.screen.equals(v.screenType())).findFirst().orElseThrow();
            if(!expected.containsKey(p.id))continue;
            assertEquals(1,ReviewedNativeWindows.compose(p,v).placements(expected.get(p.id)).size(),p.id);
            seen.add(p.id);
        }
        assertEquals(expected.keySet(),seen);
    }
    @Test void analyzerKeepsTheNativeHeaderCounterAndDecreeCellAboveThePlayerGrid() {
        var profiles=new Gson().fromJson(read("/salts_inventory_compat/native-windows/project-everything.json"),ReviewedNativeWindows.Profile[].class);
        var p=Arrays.stream(profiles).filter(a->a.id.equals("bountiful-decree-analyzer")).findFirst().orElseThrow();
        // Geometry read from the unmodified 1.21.1 publisher and actual menu.
        var decree=new DesktopWindowBounds(152,17,18,18);
        var player=new DesktopWindowBounds(8,84,18,18);
        var v=new DesktopNativeView("io.ejekta.bountiful.client.AnalyzerScreen",new DesktopWindowBounds(0,0,177,167),
            new DesktopWindowBounds(5,5,167,155),List.of(new DesktopNativeView.Slot(0,decree,false,true),new DesktopNativeView.Slot(1,player,true,true)),List.of());
        var layout=ReviewedNativeWindows.compose(p,v);
        assertEquals(1,layout.placements(decree).size());
        assertEquals(1,layout.placements(new DesktopWindowBounds(153,5,16,9)).size());
        assertTrue(layout.placements(player).isEmpty());
        assertEquals(167,layout.width());assertEquals(75,layout.height());
    }
    private static ReviewedNativeWindows.Profile smallProfile() {
        var p=new ReviewedNativeWindows.Profile();p.id="test";p.canvas=new int[]{0,0};p.clean=new int[0][];
        var r=new ReviewedNativeWindows.Region();r.source=new int[]{7,7,25,25};r.at=new int[]{0,0};
        p.regions=new ReviewedNativeWindows.Region[]{r};return p;
    }
    @Test void openRecipeDrawerKeepsItsFullNativeHeightRatherThanTheMachineCrop() {
        var p=smallProfile();p.recipeBook=true;
        var view=new DesktopNativeView("recipe-screen",new DesktopWindowBounds(0,0,176,166),
            new DesktopWindowBounds(-181,3,350,71),List.of(),List.of());
        var drawer=ReviewedNativeWindows.compose(p,view).regions().stream().filter(DesktopNativeRegion::exterior).findFirst().orElseThrow();
        assertEquals(166,drawer.source().height());
        assertEquals(-181,drawer.source().x());
    }
    @Test void extraNativeControlsShareOneColumnAndDoNotImportPackSidebarOverlays() {
        var p=smallProfile();
        var a=new DesktopNativeView.Widget("a","Button",Component.empty(),new DesktopWindowBounds(100,10,20,10),true,true);
        var b=new DesktopNativeView.Widget("b","Button",Component.empty(),new DesktopWindowBounds(130,10,20,10),true,true);
        var sidebar=new DesktopNativeView.Widget("sidebar","Sidebar",Component.empty(),new DesktopWindowBounds(-232,-142,70,36),true,true);
        var view=new DesktopNativeView("screen",new DesktopWindowBounds(0,0,176,166),new DesktopWindowBounds(7,7,162,68),List.of(),List.of(a,b,sidebar));
        var layout=ReviewedNativeWindows.compose(p,view);
        assertEquals(layout.placements(a.bounds()).get(0).bounds().x(),layout.placements(b.bounds()).get(0).bounds().x());
        assertTrue(layout.placements(sidebar.bounds()).isEmpty());
    }
    @Test void shipCargoAndPagerFollowTheNativeRowCountWithoutCopyingTheLowerInventory() {
        for(int rows=1;rows<=6;rows++) {
            var slots=new ArrayList<DesktopNativeView.Slot>();
            for(int y=0;y<rows;y++)for(int x=0;x<9;x++)slots.add(new DesktopNativeView.Slot(slots.size(),new DesktopWindowBounds(7+x*18,17+y*18,18,18),false,true));
            var player=new DesktopWindowBounds(7,30+rows*18,18,18);slots.add(new DesktopNativeView.Slot(slots.size(),player,true,true));
            var view=new DesktopNativeView("ship",new DesktopWindowBounds(0,0,256,114+rows*18),new DesktopWindowBounds(7,17,249,rows*18+16),slots,List.of());
            var layout=ReviewedNativeWindows.shipLayout(view);
            assertEquals(Math.max(108,rows*18+16),layout.height(),"The native statistics panel must remain complete beside smaller cargo holds");
            assertTrue(layout.placements(player).isEmpty());
            for(var s:slots)if(!s.player())assertEquals(1,layout.placements(s.bounds()).size());
            assertEquals(1,layout.placements(new DesktopWindowBounds(157,17+rows*18,12,12)).size());
        }
    }
}
