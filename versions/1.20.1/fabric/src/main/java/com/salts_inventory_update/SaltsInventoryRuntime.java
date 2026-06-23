package com.salts_inventory_update;

public final class SaltsInventoryRuntime {
    private static boolean configuredEnabled = true;
    private static boolean serverDesktopAvailable = true;

    private SaltsInventoryRuntime() {
    }

    public static boolean isConfiguredEnabled() {
        return configuredEnabled;
    }

    public static void setConfiguredEnabled(boolean enabled) {
        configuredEnabled = enabled;
    }

    public static void setServerDesktopAvailable(boolean available) {
        serverDesktopAvailable = available;
    }

    public static boolean isServerDesktopAvailable() {
        return serverDesktopAvailable;
    }

    public static boolean isEnabled() {
        return configuredEnabled && serverDesktopAvailable;
    }
}
