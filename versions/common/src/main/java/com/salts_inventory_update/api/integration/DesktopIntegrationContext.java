package com.salts_inventory_update.api.integration;

import java.util.Objects;
import java.util.function.Predicate;

/** Registration runs after mod registries exist, on the client or server main thread. */
public record DesktopIntegrationContext(String modId, Phase phase, Predicate<String> loadedMods) {
    public static final int API_VERSION = 6;
    public static final String DESCRIPTOR_PATH = "META-INF/salts_inventory_update.integration.properties";

    public DesktopIntegrationContext {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(loadedMods, "loadedMods");
    }

    public boolean isModLoaded(String id) {
        return loadedMods.test(id);
    }

    public enum Phase { COMMON, CLIENT }
}
