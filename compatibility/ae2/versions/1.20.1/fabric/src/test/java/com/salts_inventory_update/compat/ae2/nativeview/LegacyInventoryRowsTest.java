package com.salts_inventory_update.compat.ae2.nativeview;

import com.google.gson.Gson;
import com.salts_inventory_update.compat.ae2.nativeview.style.DesktopLayoutStyle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyInventoryRowsTest {
    @Test void nativeExtraRowRemainsInsideTheClosingBorderWithoutAccumulatingOnNavigation() {
        var style = new Gson().fromJson("{\"width\":176,\"height\":96,\"inventoryCut\":91}", DesktopLayoutStyle.class);
        style.addNativeInventoryRows(1, 18);
        style.addNativeInventoryRows(1, 18);
        assertEquals(114, style.height());
        assertEquals(114, style.minHeight());
        assertEquals(109, style.inventoryCut());
        assertEquals(176, style.width());
    }
}
