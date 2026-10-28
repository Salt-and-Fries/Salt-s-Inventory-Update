package com.salts_inventory_update.compat.ironchests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Version-scoped: other Minecraft targets do not ship this optional adapter. */
final class IronChestGridTest {
    @Test void everySlotRemainsReachableAcrossSmallAndLargeViewports() {
        for (int count : new int[]{1, 36, 54, 66, 72, 91}) {
            for (int width : new int[]{82, 162, 198, 216, 234, 360}) {
                for (int height : new int[]{18, 36, 72, 126, 216}) {
                    var layout = IronChestGrid.layout(count, width, height);
                    boolean[] reached = new boolean[count];
                    for (int row = 0; row <= layout.maxScroll(); row++) {
                        int first = layout.firstSlot(row), end = layout.endSlot(row, count);
                        assertTrue(first >= 0 && first < count);
                        assertTrue(end <= count && end > first);
                        for (int slot = first; slot < end; slot++) reached[slot] = true;
                    }
                    for (int slot = 0; slot < count; slot++) assertTrue(reached[slot], "Unreachable storage slot " + slot);
                    int usedWidth = layout.columns() * 18 + (layout.maxScroll() > 0 ? 18 : 0);
                    assertTrue(layout.gridX() + usedWidth <= width);
                    assertTrue(layout.height() <= height);
                }
            }
        }
    }

    @Test void nativeTierGridsFitWithoutScrollbarAndKeepLastSlot() {
        int[][] tiers = {{1,1}, {9,4}, {9,6}, {11,6}, {12,6}, {13,7}};
        for (int[] tier : tiers) {
            int size = tier[0] * tier[1];
            var grid = IronChestGrid.layout(size, Math.max(162, tier[0] * 18), tier[1] * 18);
            assertEquals(0, grid.maxScroll());
            assertEquals(size, grid.endSlot(0, size));
        }
    }

    @Test void shrinkingAndExpandingClampSavedScrollBeforeHitTesting() {
        var compact = IronChestGrid.layout(91, 82, 36);
        assertEquals(0, compact.firstSlot(-100));
        assertEquals(91, compact.endSlot(Integer.MAX_VALUE, 91));
        var expanded = IronChestGrid.layout(91, 234, 126);
        assertEquals(0, expanded.firstSlot(compact.maxScroll()));
        assertEquals(91, expanded.endSlot(compact.maxScroll(), 91));
    }
}
