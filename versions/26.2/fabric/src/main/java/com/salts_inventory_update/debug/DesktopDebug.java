package com.salts_inventory_update.debug;

import com.salts_inventory_update.SaltsInventoryUpdate;

public final class DesktopDebug {
    public static final boolean DEBUG = Boolean.getBoolean("salts_inventory_update.desktopDebug");
    public static final boolean TRACE = Boolean.getBoolean("salts_inventory_update.desktopTrace");

    private DesktopDebug() {
    }

    public static boolean enabled() {
        return DEBUG || TRACE;
    }

    public static boolean traceEnabled() {
        return TRACE;
    }

    public static void log(String message, Object... args) {
        if (enabled()) {
            SaltsInventoryUpdate.LOGGER.info("[desktop] " + message, args);
        }
    }

    public static void trace(String message, Object... args) {
        if (TRACE) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-trace] " + message, args);
        }
    }

    public static void warn(String message, Object... args) {
        SaltsInventoryUpdate.LOGGER.warn("[desktop] " + message, args);
    }
}
