package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TuckedTabsLayoutTest {
    @Test void iconTabsHangFromTheTopWithTheUpperBorderClipped() {
        var tabs = TuckedTabsLayout.compute(854, List.of("Inventory", "Character"));
        assertEquals(2, tabs.size());
        assertEquals(28, tabs.getFirst().width());
        assertEquals(1, tabs.getFirst().scale());
        assertEquals(216.5, tabs.getFirst().x() + tabs.getFirst().displayedWidth() / 2);
        assertEquals(637.5, tabs.getLast().x() + tabs.getLast().displayedWidth() / 2);
        assertEquals(-6, tabs.getFirst().y());
        assertEquals(24, tabs.getFirst().y() + tabs.getFirst().height() * tabs.getFirst().scale());
        assertTrue(tabs.getFirst().y() + 5 * tabs.getFirst().scale() < 0, "Five-pixel upper border is fully offscreen");
    }

    @Test void everyWindowRemainsInOrderAndWithinItsOwnCellAtCrowdedViewportSizes() {
        for (int viewport : List.of(1, 320, 854, 1920)) {
            for (int count : List.of(2, 3, 8, 24, 256)) {
                var labels = labels(count);
                var tabs = TuckedTabsLayout.compute(viewport, labels.keySet());
                assertEquals(List.copyOf(labels.keySet()), tabs.stream().map(TuckedTabsLayout.Tab::window).toList());
                double right = 0;
                for (var tab : tabs) {
                    assertTrue(tab.scale() > 0 && tab.scale() <= 1);
                    assertEquals(-6 * tab.scale(), tab.y());
                    assertTrue(tab.y() + 8 * tab.scale() >= 0, "Item icon remains below the screen edge");
                    assertTrue(tab.x() >= right);
                    right = tab.x() + tab.displayedWidth();
                    assertTrue(right <= viewport);
                }
            }
        }
    }

    @Test void crowdedTabsShrinkTogetherAndClosingAWindowRemovesOnlyItsIcon() {
        var labels = labels(2);
        double twoWidth = TuckedTabsLayout.compute(160, labels.keySet()).getFirst().displayedWidth();
        labels.putAll(labels(8));
        double eightWidth = TuckedTabsLayout.compute(160, labels.keySet()).getFirst().displayedWidth();
        assertTrue(eightWidth < twoWidth);
        labels.remove("window3");
        var tabs = TuckedTabsLayout.compute(160, labels.keySet());
        assertEquals(7, tabs.size());
        assertFalse(tabs.stream().anyMatch(tab -> tab.window().equals("window3")));
        assertEquals(List.copyOf(labels.keySet()), tabs.stream().map(TuckedTabsLayout.Tab::window).toList());
        assertTrue(TuckedTabsLayout.compute(320, List.of()).isEmpty());
    }

    private static LinkedHashMap<String, Integer> labels(int count) {
        var labels = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < count; i++) labels.put("window" + i, 100);
        return labels;
    }
}
