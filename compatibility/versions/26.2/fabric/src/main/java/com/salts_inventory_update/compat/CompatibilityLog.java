package com.salts_inventory_update.compat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CompatibilityLog {
    public static final Logger LOGGER = LoggerFactory.getLogger("salts_inventory_compat");
    private CompatibilityLog() { }
    public static void log(String message, Object... arguments) { if (com.salts_inventory_update.api.integration.DesktopEnvironment.diagnosticsEnabled()) LOGGER.info("[desktop-compat] " + message, arguments); }
    public static void trace(String message, Object... arguments) { if (com.salts_inventory_update.api.integration.DesktopEnvironment.traceDiagnosticsEnabled()) LOGGER.info("[desktop-compat-trace] " + message, arguments); }
    public static void warn(String message, Object... arguments) { LOGGER.warn(message, arguments); }
}
