package com.salts_inventory_update.compat;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.lang.reflect.InvocationTargetException;

/** Resolves optional implementations only after the owning mod is present. */
public final class CompatibilityBootstrap {
    private CompatibilityBootstrap() { }
    public static void initializeCommon() {
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("techreborn", "com.salts_inventory_update.compat.techreborn.TechRebornWindows", "initializeCommon");
        initialize("ad_astra", "com.salts_inventory_update.compat.adastra.AdAstraWindows", "initializeCommon");
        initialize("kibe", "com.salts_inventory_update.compat.kibe.KibeWindows", "initializeCommon");
        initialize("powah", "com.salts_inventory_update.compat.powah.PowahWindows", "initializeCommon");
        initialize("storagenetwork", "com.salts_inventory_update.compat.ssn.SsnWindows", "initializeCommon");
        initialize("aether", "com.salts_inventory_update.compat.aether.AetherWindows", "initializeCommon");
        initialize("enchanted", "com.salts_inventory_update.compat.enchanted.EnchantedWindows", "initializeCommon");
        initialize("quark", "com.salts_inventory_update.compat.quark.QuarkWindows", "initializeCommon");
        initialize("modern_industrialization", "com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationWindows", "initializeCommon");
        initialize("minecells", "com.salts_inventory_update.compat.minecells.MineCellsWindows", "initializeCommon");
        initialize("cataclysm", "com.salts_inventory_update.compat.cataclysm.WeaponFusionWindows", "initializeCommon");
        initialize("immersive_aircraft", "com.salts_inventory_update.compat.aircraft.AircraftWindows", "initializeCommon");
        initialize("accessories", "com.salts_inventory_update.compat.accessories.AccessoriesIntegration", "initializeCommon");
        initialize("universal-graves", "com.salts_inventory_update.compat.graves.UniversalGravesWindows", "initializeCommon");
        initialize("travelersbackpack", "com.salts_inventory_update.compat.travelersbackpack.TravelersWindows", "initializeCommon");
        initialize("toms_trading_network", "com.salts_inventory_update.compat.tomstrading.TomsTradingWindows", "initializeCommon");
        initialize("bountiful", "com.salts_inventory_update.compat.bountiful.BountifulWindows", "initializeCommon");
        initialize("tradingpost", "com.salts_inventory_update.compat.tradingpost.TradingPostWindows", "initializeCommon");
        initialize("the_bumblezone", "com.salts_inventory_update.compat.bumblezone.BumblezoneWindows", "initializeCommon");
        initialize("create", "com.salts_inventory_update.compat.create.CreateWindows", "initializeCommon");
        com.salts_inventory_update.compat.nativewindows.ReviewedNativeWindows.initializeCommon();
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
        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("techreborn", "com.salts_inventory_update.compat.techreborn.TechRebornClient", "initializeClient");
        initialize("ad_astra", "com.salts_inventory_update.compat.adastra.AdAstraClient", "initializeClient");
        initialize("kibe", "com.salts_inventory_update.compat.kibe.KibeClient", "initializeClient");
        initialize("powah", "com.salts_inventory_update.compat.powah.PowahClient", "initializeClient");
        initialize("storagenetwork", "com.salts_inventory_update.compat.ssn.SsnClient", "initializeClient");
        initialize("aether", "com.salts_inventory_update.compat.aether.AetherWindows", "initializeClient");
        initialize("enchanted", "com.salts_inventory_update.compat.enchanted.EnchantedWindows", "initializeClient");
        initialize("quark", "com.salts_inventory_update.compat.quark.QuarkWindows", "initializeClient");
        initialize("modern_industrialization", "com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationWindows", "initializeClient");
        initialize("minecells", "com.salts_inventory_update.compat.minecells.MineCellsClient", "initializeClient");
        initialize("cataclysm", "com.salts_inventory_update.compat.cataclysm.MinistrosityWindows", "initializeClient");
        initialize("cataclysm", "com.salts_inventory_update.compat.cataclysm.WeaponFusionWindows", "initializeClient");
        initialize("immersive_aircraft", "com.salts_inventory_update.compat.aircraft.AircraftWindows", "initializeClient");
        initialize("accessories", "com.salts_inventory_update.compat.accessories.AccessoriesIntegration", "initializeClient");
        initialize("universal-graves", "com.salts_inventory_update.compat.graves.UniversalGravesWindows", "initializeClient");
        initialize("travelersbackpack", "com.salts_inventory_update.compat.travelersbackpack.TravelersWindows", "initializeClient");
        initialize("toms_trading_network", "com.salts_inventory_update.compat.tomstrading.TomsTradingWindows", "initializeClient");
        initialize("bountiful", "com.salts_inventory_update.compat.bountiful.BountifulWindows", "initializeClient");
        initialize("tradingpost", "com.salts_inventory_update.compat.tradingpost.TradingPostWindows", "initializeClient");
        initialize("the_bumblezone", "com.salts_inventory_update.compat.bumblezone.BumblezoneWindows", "initializeClient");
        initialize("create", "com.salts_inventory_update.compat.create.CreateWindows", "initializeClient");
        com.salts_inventory_update.compat.nativewindows.ReviewedNativeWindows.initializeClient();
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
