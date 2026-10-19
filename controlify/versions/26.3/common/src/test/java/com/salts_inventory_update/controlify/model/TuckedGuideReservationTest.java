package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TuckedGuideReservationTest {
    @Test void settledTuckedTabsReserveTheirActualCompressedHeight() {
        Map<String, Integer> labels = new LinkedHashMap<>();
        for (int count : new int[]{2, 10, 40, 200}) {
            while (labels.size() < count) labels.put("window" + labels.size(), 130);
            var tabs = TuckedTabsLayout.compute(320, labels.keySet());
            double bottom = tabs.stream().mapToDouble(tab -> tab.y() + tab.height() * tab.scale()).max().orElse(0);
            assertEquals((int) Math.ceil(bottom + 3), TuckedGuideReservation.inset(320, 180, bottom, null, Map.of()));
            assertTrue(bottom <= 24);
            if (count == 200) assertTrue(bottom < 4, "Crowded tabs reserve their actual clipped height");
        }
    }
    @Test void largeDepartingPaneAndHighlightBorderReserveUntilTheirVisibleBottomLeavesScreen() {
        var sizes = Map.of("window", new WindowSize(176, 166));
        var frame = frame(new WindowPlacement(40, -70, 1.8, .8, 1), hiddenTuck());
        assertEquals(234, TuckedGuideReservation.inset(854, 480, 20, frame, sizes));
        var partlyGone = frame(new WindowPlacement(40, -285, 1.8, .4, 0), hiddenTuck());
        assertEquals(23, TuckedGuideReservation.inset(854, 480, 20, partlyGone, sizes));
        var fullyGone = frame(new WindowPlacement(40, -350, 1.8, 1, 1), hiddenTuck());
        assertEquals(23, TuckedGuideReservation.inset(854, 480, 20, fullyGone, sizes));
    }
    @Test void fullyTransparentOffscreenAndRemovedPreviewsDoNotReserveBand() {
        var sizes = Map.of("window", new WindowSize(176, 166));
        for (var pane : new WindowPlacement[]{new WindowPlacement(40, 30, 1, .004, 0),
                new WindowPlacement(900, 30, 1, 1, 0), new WindowPlacement(-180, 30, 1, 1, 0)})
            assertEquals(23, TuckedGuideReservation.inset(854, 480, 20, frame(pane, hiddenTuck()), sizes));
        assertEquals(23, TuckedGuideReservation.inset(854, 480, 20,
                frame(new WindowPlacement(40, 30, 1, 1, 0), hiddenTuck()), Map.of()));
    }
    @Test void tuckActionPreviewUsesItsDrawnBoundsAndInsetsStayWithinViewport() {
        var frame = frame(new WindowPlacement(20, -200, 1, 1, 0), new WindowPlacement(427, 120, 1, .5, 0));
        assertEquals(139, TuckedGuideReservation.inset(854, 480, 20, frame, Map.of("window", new WindowSize(176, 166))));
        assertEquals(180, TuckedGuideReservation.inset(320, 180, 20,
                frame(new WindowPlacement(20, 10, 2, 1, 0), hiddenTuck()), Map.of("window", new WindowSize(176, 166))));
        assertEquals(0, TuckedGuideReservation.inset(320, 180, 0, null, Map.of()));
        assertEquals(0, TuckedGuideReservation.inset(0, 180, 20, frame, Map.of()));
    }
    private static WorkspaceLayout<String> frame(WindowPlacement pane, WindowPlacement tuck) {
        return new WorkspaceLayout<>(Map.of("window", pane), tuck);
    }
    private static WindowPlacement hiddenTuck() { return new WindowPlacement(427, -50, 1, 0, 0); }
}
