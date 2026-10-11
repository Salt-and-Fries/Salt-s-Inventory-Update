package com.salts_inventory_update.integration;

import com.salts_inventory_update.api.integration.DesktopIntegrationContext.Phase;
import com.salts_inventory_update.platform.loader.api.FabricLoader;
import com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Common bootstrap: intentionally does not reference any client classes. */
public final class DesktopIntegrationBootstrap {
    private static final DesktopIntegrationLoader LOADER = new DesktopIntegrationLoader();
    private static boolean initialized;

    private DesktopIntegrationBootstrap() { }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> load(Phase.COMMON));
    }

    public static void load(Phase phase) {
        var platform = FabricLoader.getInstance();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = DesktopIntegrationBootstrap.class.getClassLoader();
        LOADER.load(platform.integrationSources(), phase, classLoader, platform::isModLoaded);
    }
}
