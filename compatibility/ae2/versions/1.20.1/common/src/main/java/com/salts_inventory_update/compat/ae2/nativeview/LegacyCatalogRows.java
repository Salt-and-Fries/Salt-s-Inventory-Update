package com.salts_inventory_update.compat.ae2.nativeview;

/** Keeps the fork's desktop minimum while preserving AE2 15's ordinary sizing. */
public final class LegacyCatalogRows {
    private LegacyCatalogRows() { }
    public static int limit(int minimum, int rows, boolean desktopHosted) {
        return desktopHosted ? Math.max(minimum, rows) : rows;
    }
}
