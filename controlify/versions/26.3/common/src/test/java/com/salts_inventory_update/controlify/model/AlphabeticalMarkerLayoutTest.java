package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AlphabeticalMarkerLayoutTest {
    private static List<AlphabeticalMarkerLayout.Rect> obstacles() {
        return List.of(new AlphabeticalMarkerLayout.Rect(12, 87, 195, 267),
            new AlphabeticalMarkerLayout.Rect(553, 87, 195, 267),
            new AlphabeticalMarkerLayout.Rect(320, 394, 120, 9),
            new AlphabeticalMarkerLayout.Rect(310, 410, 140, 9));
    }
    @Test void adjacentHILabelsAlternateInsideAndOutsideAndRetainExactTargets() {
        var groups = List.of(new AlphabeticalRecipeIndex.Group("H", 468), new AlphabeticalRecipeIndex.Group("I", 474));
        var layout = AlphabeticalMarkerLayout.layout(groups, 1249, 10, 12, obstacles());
        verify(groups, 1249, layout, obstacles());
        assertTrue(radius(layout.getFirst().labelX(), layout.getFirst().labelY()) < 144);
        assertTrue(radius(layout.get(1).labelX(), layout.get(1).labelY()) > 150);
    }
    @Test void wraparoundLettersStayApartFromSelectedCategoryHeaderAndLivePointer() {
        var groups = List.of(new AlphabeticalRecipeIndex.Group("A", 0), new AlphabeticalRecipeIndex.Group("X", 1245),
            new AlphabeticalRecipeIndex.Group("Y", 1247), new AlphabeticalRecipeIndex.Group("Z", 1248));
        var blocked = new ArrayList<>(obstacles());
        blocked.add(new AlphabeticalMarkerLayout.Rect(285, 33, 190, 39));
        blocked.add(new AlphabeticalMarkerLayout.Rect(375, 83, 10, 10));
        var layout = AlphabeticalMarkerLayout.layout(groups, 1249, 10, 12, blocked);
        verify(groups, 1249, layout, blocked);
        assertTrue(layout.stream().anyMatch(p -> p.anchorX() != p.labelX() || p.anchorY() != p.labelY()));
    }
    @Test void bottomGroupsAvoidFilterSummaryStatusPanelsAndCurrentPositionMarker() {
        var groups = List.of(new AlphabeticalRecipeIndex.Group("M", 624), new AlphabeticalRecipeIndex.Group("N", 691),
            new AlphabeticalRecipeIndex.Group("O", 708), new AlphabeticalRecipeIndex.Group("P", 750));
        var blocked = new ArrayList<>(obstacles());
        blocked.add(new AlphabeticalMarkerLayout.Rect(375, 349, 10, 10));
        verify(groups, 1249, AlphabeticalMarkerLayout.layout(groups, 1249, 10, 12, blocked), blocked);
    }
    @Test void allTwentySixDenseGroupsSurviveWithoutOverlapsOrMovedTargets() {
        var groups = new ArrayList<AlphabeticalRecipeIndex.Group>();
        for (int i = 0; i < 26; i++) groups.add(new AlphabeticalRecipeIndex.Group(Character.toString('A' + i), i));
        var blocked = new ArrayList<>(obstacles());
        blocked.add(new AlphabeticalMarkerLayout.Rect(280, 30, 200, 43));
        blocked.add(new AlphabeticalMarkerLayout.Rect(375, 83, 10, 10));
        var layout = AlphabeticalMarkerLayout.layout(groups, 100_000, 10, 12, blocked);
        verify(groups, 100_000, layout, blocked);
        assertEquals(26, layout.stream().map(AlphabeticalMarkerLayout.Placement::backgroundBounds).distinct().count());
    }
    @Test void emptyCatalogNeedsNoLabelsAndInvalidAnchorsAreRejected() {
        assertTrue(AlphabeticalMarkerLayout.layout(List.of(), 0, 10, 12, obstacles()).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> AlphabeticalMarkerLayout.layout(
            List.of(new AlphabeticalRecipeIndex.Group("A", 5)), 5, 10, 12, obstacles()));
        assertThrows(IllegalArgumentException.class, () -> AlphabeticalMarkerLayout.layout(List.of(), 0, 0, 12, obstacles()));
    }
    @Test void allLettersRemainInsideTheLargeWheelFrameIncludingNearCardinalOuterLanes() {
        var groups = new ArrayList<AlphabeticalRecipeIndex.Group>();
        for (int i = 0; i < 26; i++) groups.add(new AlphabeticalRecipeIndex.Group(Character.toString('A' + i), i * 48));
        var blocked = new ArrayList<>(obstacles());
        blocked.add(new AlphabeticalMarkerLayout.Rect(285, 41, 190, 39));
        var layout = AlphabeticalMarkerLayout.layout(groups, 1249, 10, 12, blocked);
        verify(groups, 1249, layout, blocked);
        var frame = new AlphabeticalMarkerLayout.Rect(208, 21, 344, 408);
        for (var marker : layout) assertTrue(frame.contains(marker.backgroundBounds()));
    }
    private static double radius(int x, int y) { return Math.hypot(x - 380, y - 221); }
    private static void verify(List<AlphabeticalRecipeIndex.Group> groups, int count,
            List<AlphabeticalMarkerLayout.Placement> layout, List<AlphabeticalMarkerLayout.Rect> blocked) {
        assertEquals(groups.size(), layout.size());
        for (int i = 0; i < layout.size(); i++) {
            var placement = layout.get(i);
            double angle = ContextualWheelSelection.outerMarkerAngle(groups.get(i).firstIndex(), count);
            assertEquals(380 + (int)Math.round(Math.sin(angle) * 144), placement.anchorX());
            assertEquals(221 - (int)Math.round(Math.cos(angle) * 144), placement.anchorY());
            var bounds = placement.backgroundBounds();
            assertTrue(AlphabeticalMarkerLayout.CONTENT.contains(bounds));
            for (var obstacle : blocked) assertFalse(bounds.intersects(obstacle, AlphabeticalMarkerLayout.GAP));
            for (int j = 0; j < i; j++) assertFalse(bounds.intersects(layout.get(j).backgroundBounds(), AlphabeticalMarkerLayout.GAP));
            int nearX = Math.max(bounds.x(), Math.min(380, bounds.right())), nearY = Math.max(bounds.y(), Math.min(221, bounds.bottom()));
            assertTrue(radius(nearX, nearY) >= 124);
        }
    }
    @Test void categoryTabsDoNotPushAFromItsNorthAnchor() {
        var blocked = new ArrayList<>(obstacles());
        blocked.add(new AlphabeticalMarkerLayout.Rect(362, 41, 36, 36));
        var groups = List.of(new AlphabeticalRecipeIndex.Group("A", 0), new AlphabeticalRecipeIndex.Group("B", 30),
            new AlphabeticalRecipeIndex.Group("C", 60), new AlphabeticalRecipeIndex.Group("D", 90));
        var layout = AlphabeticalMarkerLayout.layout(groups, 120, 10, 12, blocked);
        verify(groups, 120, layout, blocked);
        assertEquals(380, layout.getFirst().labelX());
        assertEquals(89, layout.getFirst().labelY());
        assertEquals(77, layout.getFirst().anchorY());
        for (int i = 0; i < layout.size(); i++) {
            double radius = radius(layout.get(i).labelX(), layout.get(i).labelY());
            assertEquals(i % 2 == 0 ? AlphabeticalMarkerLayout.INSIDE_RADIUS : AlphabeticalMarkerLayout.OUTSIDE_RADIUS, radius, 1);
        }
    }
}
