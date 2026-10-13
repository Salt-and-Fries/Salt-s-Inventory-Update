package com.salts_inventory_update.compat.ae2.nativeview;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyCatalogRowsTest {
    @Test void tinyOrdinaryScreensKeepTheNativeAe2RowCount() {
        for (int rows : new int[] {-1, 0, 1, 2, 18}) {
            assertEquals(rows, LegacyCatalogRows.limit(2, rows, false));
        }
    }
    @Test void desktopCatalogKeepsTheForkMinimumWithoutCappingLargerViews() {
        assertEquals(2, LegacyCatalogRows.limit(2, 0, true));
        assertEquals(2, LegacyCatalogRows.limit(2, 1, true));
        assertEquals(18, LegacyCatalogRows.limit(2, 18, true));
    }
}
