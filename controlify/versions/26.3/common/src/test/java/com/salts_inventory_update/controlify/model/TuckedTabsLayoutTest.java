package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TuckedTabsLayoutTest {
    @Test void permanentNamesSpreadAcrossTheTopAndShortNamesKeepTheirNaturalSize() {
        var labels = new LinkedHashMap<>(Map.of("Inventory", 54, "Character", 54));
        var tabs = TuckedTabsLayout.compute(854, labels);
        assertEquals(2, tabs.size());
        assertEquals(66, tabs.getFirst().width());
        assertEquals(1, tabs.getFirst().scale());
        assertEquals(216.5, tabs.getFirst().x() + tabs.getFirst().displayedWidth() / 2);
        assertEquals(637.5, tabs.getLast().x() + tabs.getLast().displayedWidth() / 2);
        assertEquals(2, tabs.getFirst().y());
    }

    @Test void everyWindowRemainsInOrderAndWithinItsOwnCellAtCrowdedViewportSizes() {
        for (int viewport : List.of(1, 320, 854, 1920)) {
            for (int count : List.of(2, 3, 8, 24, 256)) {
                var labels = labels(count);
                var tabs = TuckedTabsLayout.compute(viewport, labels);
                assertEquals(List.copyOf(labels.keySet()), tabs.stream().map(TuckedTabsLayout.Tab::window).toList());
                double right = 0;
                for (var tab : tabs) {
                    assertTrue(tab.scale() > 0 && tab.scale() <= 1);
                    assertTrue(tab.x() >= right);
                    right = tab.x() + tab.displayedWidth();
                    assertTrue(right <= viewport);
                }
            }
        }
    }

    @Test void addingWindowsCompressesNamesAndClosingAWindowRemovesOnlyItsTab() {
        var labels = labels(2);
        double twoWidth = TuckedTabsLayout.compute(320, labels).getFirst().displayedWidth();
        labels.putAll(labels(8));
        double eightWidth = TuckedTabsLayout.compute(320, labels).getFirst().displayedWidth();
        assertTrue(eightWidth < twoWidth);
        labels.remove("window3");
        var tabs = TuckedTabsLayout.compute(320, labels);
        assertEquals(7, tabs.size());
        assertFalse(tabs.stream().anyMatch(tab -> tab.window().equals("window3")));
        assertEquals(List.copyOf(labels.keySet()), tabs.stream().map(TuckedTabsLayout.Tab::window).toList());
        assertTrue(TuckedTabsLayout.compute(320, Map.of()).isEmpty());
    }

    private static LinkedHashMap<String, Integer> labels(int count) {
        var labels = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < count; i++) labels.put("window" + i, 100);
        return labels;
    }
}
