package com.salts_inventory_update.api.client.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DesktopWindowSurfaceTest {
    @Test void aSteppedSurfaceHasAnExplicitHoleIndependentOfItsAlpha() {
        var surface = DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 176, 72),
            DesktopWindowRegion.visualAndInteractive(0, 72, 152, 70)));
        assertEquals(DesktopHitRole.CONTENT, surface.hitRole(100, 80, 176, 142, 260, 100));
        assertEquals(DesktopHitRole.NONE, surface.hitRole(100, 80, 176, 142, 260, 200));
        assertFalse(surface.occludes(100, 80, 176, 142, 260, 200));
        assertTrue(surface.occludes(100, 80, 176, 142, 140, 200));
        assertEquals(DesktopInsets.ZERO, surface.insets());
        assertEquals(DesktopSurfaceMode.MOD_OWNED, surface.mode());
    }

    @Test void controlsAndSlotsTakePriorityOverDragAndResize() {
        var surface = DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 160, 120),
            DesktopWindowRegion.interactive(0, 0, 160, 20).withRole(DesktopHitRole.MOVE),
            DesktopWindowRegion.interactive(140, 0, 20, 20).withRole(DesktopHitRole.RESIZE_NORTH_EAST),
            DesktopWindowRegion.visualAndInteractive(140, 0, 20, 20).withRole(DesktopHitRole.CONTROL),
            DesktopWindowRegion.interactive(100, 0, 18, 18).withRole(DesktopHitRole.SLOT)));
        assertEquals(DesktopHitRole.MOVE, surface.hitRole(0, 0, 160, 120, 80, 10));
        assertEquals(DesktopHitRole.CONTROL, surface.hitRole(0, 0, 160, 120, 150, 10));
        assertEquals(DesktopHitRole.SLOT, surface.hitRole(0, 0, 160, 120, 110, 10));
        assertEquals(DesktopHitRole.CONTENT, surface.hitRole(0, 0, 160, 120, 80, 60));
    }

    @Test void visualOnlyAndTransientRegionsHaveSeparateResponsibilities() {
        var surface = DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visual(-12, 0, 12, 60),
            DesktopWindowRegion.transientVisualAndInteractive(160, 0, 80, 60)));
        assertEquals(DesktopHitRole.NONE, surface.hitRole(20, 10, 160, 120, 12, 20));
        assertTrue(surface.occludes(20, 10, 160, 120, 12, 20));
        assertEquals(DesktopHitRole.POPUP, surface.hitRole(20, 10, 160, 120, 210, 30));
        assertFalse(surface.regions().get(1).constrainsWindow());
    }

    @Test void layoutIsADefensiveSnapshotAndCanBeSharedSafely() {
        var source = new ArrayList<DesktopWindowRegion>();
        source.add(DesktopWindowRegion.visualAndInteractive(0, 0, 40, 40));
        var surface = DesktopWindowSurface.modOwned(source);
        source.clear();
        assertEquals(1, surface.regions().size());
        assertThrows(UnsupportedOperationException.class, () -> surface.regions().clear());
        var next = surface.withRegions(List.of(DesktopWindowRegion.interactive(40, 0, 20, 20)));
        assertEquals(1, surface.regions().size());
        assertEquals(2, next.regions().size());
    }

    @Test void insetsGiveExactContentSurfaceConversions() {
        var insets = new DesktopInsets(3, 15, 5, 7);
        var surface = insets.surfaceSize(176, 73);
        assertEquals(184, surface.width());
        assertEquals(95, surface.height());
        assertEquals(176, insets.contentWidth(surface.width()));
        assertEquals(73, insets.contentHeight(surface.height()));
        assertEquals(DesktopWindowSize.of(176, 73), DesktopInsets.ZERO.surfaceSize(176, 73));
        assertThrows(IllegalArgumentException.class, () -> new DesktopInsets(-1, 0, 0, 0));
        assertThrows(ArithmeticException.class, () -> new DesktopInsets(Integer.MAX_VALUE, 0, 1, 0));
        assertThrows(ArithmeticException.class, () -> insets.surfaceSize(Integer.MAX_VALUE, 20));
    }

    @Test void drawingAndInputUseTheSameTransformIncludingNegativeOrigins() {
        var transform = new DesktopTransform(-20, 37, 1.5);
        assertEquals(40, transform.screenX(40));
        assertEquals(52, transform.screenY(10));
        assertEquals(40, transform.localX(40));
        assertEquals(10, transform.localY(52));
        assertThrows(IllegalArgumentException.class, () -> new DesktopTransform(0, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new DesktopTransform(0, 0, 0));
    }

    @Test void invalidRegionsCannotOverflowHitCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> DesktopWindowRegion.interactive(0, 0, -1, 10));
        assertThrows(ArithmeticException.class, () -> DesktopWindowRegion.visual(Integer.MAX_VALUE, 0, 2, 1));
    }

    @Test void resizingFromTopLeftPreservesTheOppositeCornerAndHonorsMinimums() {
        var bounds = new DesktopWindowBounds(100, 80, 160, 120);
        var resized = bounds.resizeGesture(DesktopHitRole.RESIZE_NORTH_WEST, 30, 20, 100, 80, 500, 500);
        assertEquals(new DesktopWindowBounds(130, 100, 130, 100), resized);
        var minimum = bounds.resizeGesture(DesktopHitRole.RESIZE_NORTH_WEST, 200, 200, 100, 80, 500, 500);
        assertEquals(new DesktopWindowBounds(160, 120, 100, 80), minimum);
        assertEquals(260, minimum.x() + minimum.width());
        assertEquals(200, minimum.y() + minimum.height());
    }

    @Test void resizingOneEdgeLeavesTheOtherAxisUntouched() {
        var bounds = new DesktopWindowBounds(100, 80, 160, 120);
        assertEquals(new DesktopWindowBounds(100, 80, 210, 120),
            bounds.resizeGesture(DesktopHitRole.RESIZE_EAST, 50, 500, 100, 80, 500, 500));
        assertEquals(new DesktopWindowBounds(100, 50, 160, 150),
            bounds.resizeGesture(DesktopHitRole.RESIZE_NORTH, 500, -30, 100, 80, 500, 500));
        assertThrows(IllegalArgumentException.class, () -> bounds.resizeGesture(DesktopHitRole.MOVE, 1, 1, 1, 1, 500, 500));
    }

    @Test void chromeMetricsConvertContentAndRejectControlsOutsideTheTitleBand() {
        var metrics = new DesktopChromeMetrics(30, 3, 4, 20, 16, 4, 2, 8, 7);
        assertEquals(74, metrics.controlsWidth(3));
        assertEquals(new DesktopInsets(6, 36, 6, 6), metrics.contentInsets(6));
        assertThrows(IllegalArgumentException.class, () -> new DesktopChromeMetrics(12, 3, 4, 20, 16, 4, 2, 8, 7));
        assertThrows(ArithmeticException.class, () -> metrics.controlsWidth(Integer.MAX_VALUE));
    }
}
