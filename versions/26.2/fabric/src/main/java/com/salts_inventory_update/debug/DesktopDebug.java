package com.salts_inventory_update.debug;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.SaltsInventoryRuntime;

public final class DesktopDebug {
    public static final boolean DEBUG = Boolean.getBoolean("salts_inventory_update.desktopDebug");
    public static final boolean TRACE = Boolean.getBoolean("salts_inventory_update.desktopTrace");

    private DesktopDebug() {
    }

    public static boolean enabled() {
        return DEBUG || TRACE || SaltsInventoryRuntime.detailedConsoleLogs();
    }

    public static boolean traceEnabled() {
        return TRACE || SaltsInventoryRuntime.detailedConsoleLogs();
    }

    public static void log(String message, Object... args) {
        if (enabled()) {
            SaltsInventoryUpdate.LOGGER.info("[desktop] " + message, args);
        }
    }

    public static void trace(String message, Object... args) {
        if (traceEnabled()) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-trace] " + message, args);
        }
    }

    public static void detail(String message, Object... args) {
        if (SaltsInventoryRuntime.detailedConsoleLogs()) {
            SaltsInventoryUpdate.LOGGER.info("[desktop-detail] " + message, args);
        }
    }

    public static void warn(String message, Object... args) {
        SaltsInventoryUpdate.LOGGER.warn("[desktop] " + message, args);
    }
}
