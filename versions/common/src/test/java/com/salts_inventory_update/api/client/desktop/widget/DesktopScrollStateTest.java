package com.salts_inventory_update.api.client.desktop.widget;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class DesktopScrollStateTest {
    @Test
    void fractionalScrollingUsesTheSameCoordinatesForRenderingAndHitTesting() {
        var state = new DesktopScrollState();
        state.layout(100, 20, 50);
        assertTrue(state.scroll(25.5));
        assertEquals(new DesktopScrollState.VisibleRows(1, 4), state.visibleRows(0));
        assertEquals(-5.5, state.rowY(1));
        assertEquals(1, state.rowAt(0));
        assertEquals(2, state.rowAt(14.5));
        assertEquals(-1, state.rowAt(50));
        assertEquals(-1, state.rowAt(-1));
    }

    @Test
    void changesToTheModelAndViewportClampScrollWithoutPhantomRows() {
        var state = new DesktopScrollState();
        state.layout(100, 20, 60);
        state.offset(10000);
        assertEquals(1940, state.offset());
        state.layout(2, 20, 60);
        assertEquals(0, state.offset());
        assertEquals(new DesktopScrollState.VisibleRows(0, 2), state.visibleRows(4));
        assertEquals(-1, state.rowAt(45));
        assertFalse(state.scroll(30));
    }

    @Test
    void veryLargeVirtualListsDoNotOverflowPixelOrRowCalculations() {
        var state = new DesktopScrollState();
        state.layout(Integer.MAX_VALUE, 100, 1000);
        state.offset(Double.MAX_VALUE);
        assertEquals(new DesktopScrollState.VisibleRows(Integer.MAX_VALUE - 12, Integer.MAX_VALUE), state.visibleRows(2));
        assertEquals(Integer.MAX_VALUE - 1, state.rowAt(999));
    }

    @Test
    void invalidGeometryAndOffsetsAreRejected() {
        var state = new DesktopScrollState();
        assertThrows(IllegalArgumentException.class, () -> state.layout(-1, 20, 100));
        assertThrows(IllegalArgumentException.class, () -> state.layout(20, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> state.offset(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> state.visibleRows(-1));
        state.layout(10, 20, 0);
        assertEquals(new DesktopScrollState.VisibleRows(0, 0), state.visibleRows(0));
    }
}
