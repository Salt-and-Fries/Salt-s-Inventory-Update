package com.salts_inventory_update;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SaltsInventoryRuntime {
    private static boolean configuredEnabled = true;
    private static boolean serverDesktopAvailable = true;
    private static boolean detailedConsoleLogs = false;
    private static Set<String> forcedContainerWindowIds = Set.of();

    private SaltsInventoryRuntime() {
    }

    public static boolean isConfiguredEnabled() {
        return configuredEnabled;
    }

    public static void setConfiguredEnabled(boolean enabled) {
        configuredEnabled = enabled;
    }

    public static boolean detailedConsoleLogs() {
        return detailedConsoleLogs;
    }

    public static void setDetailedConsoleLogs(boolean enabled) {
        detailedConsoleLogs = enabled;
    }

    public static void setForcedContainerWindowIds(Collection<String> ids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalized.add(id.trim());
            }
        }
        forcedContainerWindowIds = Collections.unmodifiableSet(normalized);
    }

    public static boolean isForcedContainerWindow(String menuId) {
        return menuId != null && forcedContainerWindowIds.contains(menuId);
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
