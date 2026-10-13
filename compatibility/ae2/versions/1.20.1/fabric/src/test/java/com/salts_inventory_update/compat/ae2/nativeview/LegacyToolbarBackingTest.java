package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;
import com.salts_inventory_update.compat.ae2.client.DesktopChrome;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LegacyToolbarBackingTest {
    private static final class Canvas implements DesktopChrome.Graphics {
        final int width; final int[] pixels; int fills;
        Canvas(int width, int height) { this.width = width; pixels = new int[width * height]; }
        @Override public void fill(int x0, int y0, int x1, int y1, int color) {
            fills++;
            for (int y = y0; y < y1; y++) Arrays.fill(pixels, y * width + x0, y * width + x1, color);
        }
        @Override public void sprite(String id, DesktopChrome.Bounds bounds) { fail("No native texture should be bundled or synthesized"); }
    }

    @Test void everyForkUsedRegionMatchesTheReferenceSpritePixels() {
        // These six swatches were measured from the unmodified reference-era
        // AE2 vertical_buttons_bg sprite. No image resource is copied.
        int edge = 0xFF413F54, light = 0xFFF2F2F2, shade = 0xFF878FA5, face = 0xFFCBCCD4;
        int[][] regions = {{2,2,1,1},{5,0,1,2},{5,22,1,4},{0,2,2,1},{0,22,2,4},{0,0,2,2}};
        int[][] expected = {{face},{edge,light},{light,shade,shade,edge},{edge,light},
                {edge,light,edge,shade,edge,shade,edge,edge},{edge,edge,edge,light}};
        for (int i = 0; i < regions.length; i++) {
            int[] r = regions[i]; var canvas = new Canvas(r[2], r[3]);
            LegacyToolbarBacking.paint(canvas, r[0], r[1], r[2], r[3], 0, 0, r[2], r[3]);
            assertArrayEquals(expected[i], canvas.pixels, Arrays.toString(r));
        }
    }

    @Test void stretchedInteriorBatchesItsUniformPixelsIntoOneDraw() {
        var canvas = new Canvas(200, 400);
        LegacyToolbarBacking.paint(canvas, 2, 2, 1, 1, 0, 0, 200, 400);
        assertEquals(1, canvas.fills);
        assertTrue(Arrays.stream(canvas.pixels).allMatch(color -> color == 0xFFCBCCD4));
    }
}
