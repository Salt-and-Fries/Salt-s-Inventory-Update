package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeWindowPolishTest {
    private DesktopWindowBounds rect(int x, int y, int w, int h) {
        return new DesktopWindowBounds(x, y, w, h);
    }

    private DesktopNativeView grid(int columns, int rows) {
        var slots = new ArrayList<DesktopNativeView.Slot>();
        for (int i = 0; i < columns * rows; i++) {
            slots.add(new DesktopNativeView.Slot(i, rect(7 + i % columns * 18, 18 + i / columns * 18, 18, 18), false, true));
        }
        slots.add(new DesktopNativeView.Slot(slots.size(), rect(7, 180, 18, 18), true, true));
        return new DesktopNativeView("native", rect(0, 0, Math.max(176, columns * 18 + 16), 280),
            rect(7, 16, Math.max(162, columns * 18), rows * 18 + 9), slots, List.of());
    }

    @Test void storageCropPreservesEveryOriginalSlotAndRemovesOnlyUnusedBands() {
        for (int[] shape : new int[][]{{3,1},{9,1},{9,2},{9,3},{9,5},{9,6},{11,6},{15,6},{5,4}}) {
            var view = grid(shape[0], shape[1]);
            var layout = NativeWindowPolish.slots(view);
            assertTrue(layout.scrollable());
            assertEquals(0, layout.padding());
            assertEquals(shape[0]*18,layout.width());assertEquals(shape[1]*18,layout.height());
            for (var slot : view.slots()) if (!slot.player()) {
                var placement = layout.placements(slot.bounds());
                assertEquals(1, placement.size());
                var b = placement.get(0).bounds();
                var point = layout.sourceAt(b.x() + 9, b.y() + 9).orElseThrow();
                assertEquals(slot.bounds().x() + 9, point.x());
                assertEquals(slot.bounds().y() + 9, point.y());
            }
            assertTrue(layout.placements(view.slots().get(view.slots().size() - 1).bounds()).isEmpty());
            assertEquals(shape[1] * 18, layout.regions().get(0).source().height());
        }
    }

    @Test void kilnPreviewUsesObservedAnimationHeightAndKeepsAllHatches() {
        var cells = List.of(new DesktopNativeView.Slot(0, rect(31, 16, 18, 18), false, true),
            new DesktopNativeView.Slot(1, rect(31, 52, 18, 18), false, true),
            new DesktopNativeView.Slot(2, rect(111, 38, 18, 18), false, true));
        var widgets = List.of(new DesktopNativeView.Widget("left", "native", Component.empty(), rect(86,40,14,14),true,true),
            new DesktopNativeView.Widget("back","native",Component.empty(),rect(113,14,14,14),true,true),
            new DesktopNativeView.Widget("right","native",Component.empty(),rect(140,40,14,14),true,true));
        for (int height : new int[]{0,8,12,25,45}) {
            var view = new DesktopNativeView("native", rect(0, 0, 176, 166), rect(7, -height, 162, 72 + height), cells, widgets);
            var layout = NativeWindowPolish.kiln(view);
            assertEquals(2, layout.regions().size());
            var body = layout.regions().get(0);
            var preview = layout.regions().get(1);
            int visibleHeight=Math.max(8,height);
            assertEquals(visibleHeight, preview.source().height());
            assertEquals(92, preview.source().width());
            assertEquals(0, body.y());assertEquals(0,body.x());
            assertEquals(123,layout.width());assertEquals(56,layout.height());assertTrue(preview.exterior());
            assertEquals(-visibleHeight,preview.y());assertEquals(23,preview.x());
            for(var widget:widgets)assertTrue(body.covers(widget.bounds()));
            for (var slot : cells) assertTrue(body.covers(slot.bounds()));
            var nativePoint = layout.exteriorSourceAt(preview.x()+5,-visibleHeight+2).orElseThrow();
            assertEquals(preview.source().x() + 5, nativePoint.x());
            assertEquals(-visibleHeight + 2, nativePoint.y());
        }
    }

    @Test void conservativeFallbackKeepsArtworkWithoutDuplicatingFramePadding() {
        var view = grid(9, 3);
        var layout = NativeWindowPolish.machine(view);
        assertEquals(view.automaticBounds(), layout.regions().get(0).source());
        assertEquals(0, layout.padding());
    }

    @Test void machineCropsIncludeDecorativeFramesAndTheRecipeButton(){
        var slots=List.of(new DesktopNativeView.Slot(0,rect(55,16,18,18),false,true),
            new DesktopNativeView.Slot(1,rect(55,52,18,18),false,true),new DesktopNativeView.Slot(2,rect(115,34,18,18),false,true));
        var book=new DesktopNativeView.Widget("book","button",Component.empty(),rect(20,34,20,18),true,true);
        var view=new DesktopNativeView("native",rect(0,0,176,166),rect(7,16,162,56),slots,List.of(book));
        assertEquals(rect(20,16,117,54),NativeWindowPolish.oxidized(view).regions().get(0).source());
        var jslots=List.of(new DesktopNativeView.Slot(0,rect(47,24,18,18),false,true),new DesktopNativeView.Slot(1,rect(47,42,18,18),false,true),
            new DesktopNativeView.Slot(2,rect(79,59,18,18),false,true),new DesktopNativeView.Slot(3,rect(115,34,18,18),false,true),new DesktopNativeView.Slot(4,rect(115,59,18,18),false,true));
        var jview=new DesktopNativeView("native",rect(0,0,176,166),rect(7,16,162,63),jslots,List.of());
        assertEquals(rect(39,16,98,61),NativeWindowPolish.juicer(jview).regions().get(0).source());
    }
}
