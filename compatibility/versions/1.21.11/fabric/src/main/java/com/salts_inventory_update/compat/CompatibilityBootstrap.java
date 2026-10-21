package com.salts_inventory_update.compat;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.lang.reflect.InvocationTargetException;

/** Resolves optional implementations only after the owning mod is present. */
public final class CompatibilityBootstrap {
    private CompatibilityBootstrap() { }
    public static void initializeCommon() {
        initialize("toms_storage", "com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat", "initialize");
    }
    public static void initializeClient() {
        initialize("raised", "com.salts_inventory_update.compat.raised.RaisedHudIntegration", "initialize");
        initialize("toms_storage", "com.salts_inventory_update.compat.toms_storage.client.TomsStorageClientCompat", "initialize");
    }
    public static void initialize(String modId, String implementation, String method) {
        initialize(modId, implementation, method, DesktopEnvironment::isModLoaded);
    }
    static void initialize(String modId, String implementation, String method, java.util.function.Predicate<String> loadedMods) {
        if (!loadedMods.test(modId)) return;
        try {
            DesktopEnvironment.registerIntegration(modId, () -> {
                try { Class.forName(implementation).getMethod(method).invoke(null); }
                catch (ReflectiveOperationException failure) {
                    Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
                    throw new IllegalStateException("Integration initialization failed: " + implementation, cause);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            CompatibilityLog.LOGGER.error("Unable to initialize optional integration {}", modId, failure);
        }
    }
}
