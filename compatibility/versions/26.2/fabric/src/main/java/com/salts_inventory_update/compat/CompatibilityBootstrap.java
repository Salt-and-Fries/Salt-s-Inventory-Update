package com.salts_inventory_update.compat;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.lang.reflect.InvocationTargetException;

/** Resolves optional implementations only after the owning mod is present. */
public final class CompatibilityBootstrap {
    private CompatibilityBootstrap() { }
    public static void initializeCommon() {
        initialize("graveyard", "com.salts_inventory_update.compat.graveyard.OssuarySync", "initializeCommon");
        initialize("animal_feeding_trough", "com.salts_inventory_update.compat.feedingtrough.FeedingTroughIntegration", "initializeCommon");
        initialize("ae2", "com.salts_inventory_update.compat.ae2.AE2Bootstrap", "initializeCommon");
        initialize("toms_storage", "com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat", "initialize");
    }
    public static void initializeClient() {
        initialize("graveyard", "com.salts_inventory_update.compat.graveyard.GraveyardNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("betterend", "com.salts_inventory_update.compat.betterend.BetterEndNativeUi", "initialize");
        initialize("runes", "com.salts_inventory_update.compat.runes.RunesNativeUi", "initialize");
        initialize("barteringstation", "com.salts_inventory_update.compat.barteringstation.BarteringStationNativeUi", "initialize");
        initialize("farmersdelight", "com.salts_inventory_update.compat.farmersdelight.FarmersDelightNativeUi", "initialize");
        initialize("betterarcheology", "com.salts_inventory_update.compat.betterarcheology.BetterArcheologyNativeUi", "initialize");
        initialize("illagerinvasion", "com.salts_inventory_update.compat.illagerinvasion.IllagerInvasionNativeUi", "initialize");
        initialize("alloyed", "com.salts_inventory_update.compat.alloyed.AlloyedNativeUi", "initialize");
        initialize("animal_feeding_trough", "com.salts_inventory_update.compat.feedingtrough.FeedingTroughClientIntegration", "initialize");
        initialize("netherchested", "com.salts_inventory_update.compat.netherchested.NetherChestedNativeUi", "initialize");
        initialize("raised", "com.salts_inventory_update.compat.raised.RaisedHudIntegration", "initialize");
        initialize("ae2", "com.salts_inventory_update.compat.ae2.AE2Bootstrap", "initializeClient");
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
