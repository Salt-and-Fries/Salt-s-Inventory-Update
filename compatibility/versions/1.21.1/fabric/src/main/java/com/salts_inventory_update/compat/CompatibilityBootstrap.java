package com.salts_inventory_update.compat;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.lang.reflect.InvocationTargetException;

/** Resolves optional implementations only after the owning mod is present. */
public final class CompatibilityBootstrap {
    private CompatibilityBootstrap() { }
    public static void initializeCommon() {
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("extendeddrawersaddon", "com.salts_inventory_update.compat.extendeddrawersaddon.ExtendedDrawersAddonIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("tiered", "com.salts_inventory_update.compat.tieredz.TieredZIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("smitherz", "com.salts_inventory_update.compat.smitherz.SmitherZIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("additionz", "com.salts_inventory_update.compat.additionz.AdditionZIntegration", "initializeCommon");
        initialize("chipped", "com.salts_inventory_update.compat.chipped.ChippedIntegration", "initializeCommon");
        initialize("simplyswords", "com.salts_inventory_update.compat.simplyswords.SimplySwordsIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("inmis", "com.salts_inventory_update.compat.inmis.InmisIntegration", "initializeCommon");
        initialize("animal_feeding_trough", "com.salts_inventory_update.compat.feedingtrough.FeedingTroughIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("trinkets", "com.salts_inventory_update.compat.trinkets.TrinketsIntegration", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("sophisticatedcore", "com.salts_inventory_update.compat.sophisticated.SophisticatedFabricBootstrap", "initializeCommon");
        if (DesktopEnvironment.isModLoaded("neoforge"))
            initialize("ae2", "com.salts_inventory_update.compat.ae2.AE2Bootstrap", "initializeCommon");
        initialize("toms_storage", "com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat", "initialize");
    }
    public static void initializeClient() {
        initialize("graveyard", "com.salts_inventory_update.compat.graveyard.GraveyardNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("betterend", "com.salts_inventory_update.compat.betterend.BetterEndNativeUi", "initialize");
        initialize("runes", "com.salts_inventory_update.compat.runes.RunesNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("neoforge")) initialize("deep_aether", "com.salts_inventory_update.compat.deepaether.DeepAetherNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("extendeddrawersaddon", "com.salts_inventory_update.compat.extendeddrawersaddon.ExtendedDrawersAddonIntegration", "initializeClient");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("tiered", "com.salts_inventory_update.compat.tieredz.TieredZIntegration", "initializeClient");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("smitherz", "com.salts_inventory_update.compat.smitherz.SmitherZIntegration", "initializeClient");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("additionz", "com.salts_inventory_update.compat.additionz.AdditionZIntegration", "initializeClient");
        initialize("chipped", "com.salts_inventory_update.compat.chipped.ChippedClient", "initialize");
        initialize("simplyswords", "com.salts_inventory_update.compat.simplyswords.SimplySwordsClient", "initialize");
        initialize("barteringstation", "com.salts_inventory_update.compat.barteringstation.BarteringStationNativeUi", "initialize");
        initialize("alloy_forgery", "com.salts_inventory_update.compat.alloyforgery.AlloyForgeryNativeUi", "initialize");
        initialize("seasonsextras", "com.salts_inventory_update.compat.seasonsextras.SeasonsExtrasNativeUi", "initialize");
        initialize("farmersdelight", "com.salts_inventory_update.compat.farmersdelight.FarmersDelightNativeUi", "initialize");
        initialize("minersdelight", "com.salts_inventory_update.compat.minersdelight.MinersDelightNativeUi", "initialize");
        initialize("betterarcheology", "com.salts_inventory_update.compat.betterarcheology.BetterArcheologyNativeUi", "initialize");
        initialize("illagerinvasion", "com.salts_inventory_update.compat.illagerinvasion.IllagerInvasionNativeUi", "initialize");
        initialize("galosphere", "com.salts_inventory_update.compat.galosphere.GalosphereNativeUi", "initialize");
        initialize("sawmill", "com.salts_inventory_update.compat.sawmill.SawmillNativeUi", "initialize");
        initialize("alloyed", "com.salts_inventory_update.compat.alloyed.AlloyedNativeUi", "initialize");
        initialize("animal_feeding_trough", "com.salts_inventory_update.compat.feedingtrough.FeedingTroughClientIntegration", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("oxidized", "com.salts_inventory_update.compat.oxidized.OxidizedNativeUi", "initialize");
        initialize("brick_and_mortar", "com.salts_inventory_update.compat.brickmortar.BrickMortarNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("neoforge")) initialize("expandeddelight", "com.salts_inventory_update.compat.expandeddelight.ExpandedDelightNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("neoforge")) initialize("crabbersdelight", "com.salts_inventory_update.compat.crabbersdelight.CrabbersDelightNativeUi", "initialize");
        initialize("netherchested", "com.salts_inventory_update.compat.netherchested.NetherChestedNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("inmis", "com.salts_inventory_update.compat.inmis.InmisNativeUi", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("trinkets", "com.salts_inventory_update.compat.trinkets.TrinketsIntegration", "initializeClient");
        initialize("raised", "com.salts_inventory_update.compat.raised.RaisedHudIntegration", "initialize");
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("sophisticatedcore", "com.salts_inventory_update.compat.sophisticated.SophisticatedFabricBootstrap", "initializeClient");
        if (DesktopEnvironment.isModLoaded("neoforge"))
            initialize("ae2", "com.salts_inventory_update.compat.ae2.AE2Bootstrap", "initializeClient");
        initialize("toms_storage", "com.salts_inventory_update.compat.toms_storage.client.TomsStorageClientCompat", "initialize");
        initialize("emi", "com.salts_inventory_update.compat.emi.EmiRecipeBrowserBootstrap", "initialize");
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
