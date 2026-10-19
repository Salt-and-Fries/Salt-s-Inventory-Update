package com.salts_inventory_update.controlify.model;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CompactGuideLayoutTest {
    @Test void ordinaryActionsUseOneUnscaledHorizontalRow() {
        var layout = CompactGuideLayout.compute(sizes(8, 64, 16), 854, 480, 1);
        assertEquals(1, layout.rows());
        assertEquals(1, layout.scale());
        assertEquals(25, layout.reservedHeight());
        assertFits(layout, 854, 480);
    }
    @Test void narrowViewportWrapsRatherThanOverShrinkingNativeGlyphs() {
        var layout = CompactGuideLayout.compute(sizes(8, 64, 16), 320, 240, 1);
        assertEquals(2, layout.rows());
        assertEquals(1, layout.scale());
        assertEquals(44, layout.reservedHeight());
        assertFits(layout, 320, 240);
    }
    @Test void nativeRecipeGuideWithCraftStackUsesOneRowBelowTheHealthHud() {
        // Widths observed from the real 26.3 recipe guide at 1280x900, GUI scale 2.
        var layout = CompactGuideLayout.compute(recipeGuide(), 640, 450, 1);
        assertEquals(1, layout.rows());
        assertTrue(layout.scale() >= .9 && layout.scale() < 1);
        assertTrue(layout.top() >= 429, "A second footer row must not overlap the health HUD");
        assertTrue(layout.reservedHeight() <= 25);
        assertFits(layout, 640, 450);
    }
    @Test void nativeRecipeGuideStillUsesTwoReadableRowsInAConstrainedViewport() {
        var layout = CompactGuideLayout.compute(recipeGuide(), 400, 300, 1);
        assertEquals(2, layout.rows());
        assertEquals(1, layout.scale());
        assertTrue(layout.reservedHeight() <= 60);
        assertFits(layout, 400, 300);
    }
    @Test void modestSingleRowReductionRespectsTheConfiguredGuideScale() {
        var layout = CompactGuideLayout.compute(recipeGuide(), 1140, 800, 2);
        assertEquals(1, layout.rows());
        assertTrue(layout.scale() >= 1.6 && layout.scale() < 2);
        assertFits(layout, 1140, 800);
    }
    @Test void LargePreferenceAndManyTranslatedActionsStayWithinTwoRowsAndTwentyPercentBand() {
        for (double width : new double[]{160, 320, 854, 1920}) for (double height : new double[]{100, 180, 480, 1080}) {
            var layout = CompactGuideLayout.compute(sizes(12, 105, 16), width, height, 2.5);
            assertTrue(layout.rows() <= 2);
            assertTrue(layout.scale() <= 2.5);
            assertTrue(layout.reservedHeight() <= Math.ceil(height * .20));
            assertFits(layout, width, height);
        }
    }
    @Test void MixedGlyphHeightsAlignWithinEachRowWithoutOverlap() {
        var layout = CompactGuideLayout.compute(List.of(new CompactGuideLayout.Size(80, 9),
            new CompactGuideLayout.Size(70, 24), new CompactGuideLayout.Size(90, 16),
            new CompactGuideLayout.Size(60, 9), new CompactGuideLayout.Size(130, 20)), 280, 360, .75);
        assertFits(layout, 280, 360);
        assertEquals(5, layout.entries().size());
        for (var first : layout.entries()) for (var second : layout.entries()) if (first.index() < second.index()) {
            if (first.row() == second.row()) assertTrue(first.x() + first.width() <= second.x());
            else assertTrue(first.y() + first.height() <= second.y());
        }
    }
    @Test void EmptyGuidesReserveNoInventorySpace() {
        var layout = CompactGuideLayout.compute(List.of(), 854, 480, 1);
        assertEquals(0, layout.rows()); assertEquals(0, layout.reservedHeight());
        assertTrue(layout.entries().isEmpty());
    }
    private static List<CompactGuideLayout.Size> recipeGuide() {
        return IntStream.of(63, 55, 47, 78, 57, 57, 74, 88, 69)
            .mapToObj(width -> new CompactGuideLayout.Size(width, 15)).toList();
    }
    private static List<CompactGuideLayout.Size> sizes(int count, int width, int height) {
        return IntStream.range(0, count).mapToObj(i -> new CompactGuideLayout.Size(width, height)).toList();
    }
    private static void assertFits(CompactGuideLayout.Layout layout, double width, double height) {
        for (var entry : layout.entries()) {
            assertTrue(entry.x() >= 0); assertTrue(entry.y() >= 0);
            assertTrue(entry.x() + entry.width() <= width + 1e-8);
            assertTrue(entry.y() + entry.height() <= height - CompactGuideLayout.BOTTOM_MARGIN + 1e-8);
            assertTrue(entry.y() >= height - layout.reservedHeight());
        }
    }
}
