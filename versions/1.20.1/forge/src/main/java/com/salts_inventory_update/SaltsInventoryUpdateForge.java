package com.salts_inventory_update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.platform.ForgePlatform;
import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(SaltsInventoryUpdate.MOD_ID)
public final class SaltsInventoryUpdateForge {
    public SaltsInventoryUpdateForge() {
        logForgeDiagnostics();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgePlatform.initialize(modBus);

        SaltsInventoryUpdate.init("Forge " + VersionInfo.MINECRAFT_VERSION);
        DesktopPackets.registerPayloadTypes();
        DesktopContainerSessions.initialize();
        TomsStorageServerCompat.initialize();

        ForgePlatform.initializeClient(modBus);
    }

    private static void logForgeDiagnostics() {
        if (!Boolean.getBoolean("salts_inventory_update.desktopDebug") && !Boolean.getBoolean("salts_inventory_update.desktopTrace")) {
            return;
        }

        SaltsInventoryUpdate.LOGGER.info(
            "[forge-diagnostics] constructor begin mc={} thread={} classLoader={} contextClassLoader={} desktopDebug={} desktopTrace={}",
            VersionInfo.MINECRAFT_VERSION,
            Thread.currentThread().getName(),
            SaltsInventoryUpdateForge.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            Boolean.getBoolean("salts_inventory_update.desktopDebug"),
            Boolean.getBoolean("salts_inventory_update.desktopTrace")
        );
        logResources("salts_inventory_update.mixins.json");
        logResources("META-INF/mods.toml");
        logMixinManifests();
    }

    private static void logResources(String resourceName) {
        ClassLoader loader = contextLoader();
        int count = 0;
        try {
            Enumeration<URL> resources = loader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                count++;
                URL resource = resources.nextElement();
                SaltsInventoryUpdate.LOGGER.info("[forge-diagnostics] resource {} #{} -> {}", resourceName, count, resource);
            }
        } catch (IOException exception) {
            SaltsInventoryUpdate.LOGGER.warn("[forge-diagnostics] resource {} lookup failed", resourceName, exception);
            return;
        }

        if (count == 0) {
            SaltsInventoryUpdate.LOGGER.warn("[forge-diagnostics] resource {} not found on context classpath", resourceName);
        }
    }

    private static void logMixinManifests() {
        ClassLoader loader = contextLoader();
        int count = 0;
        int mixinConfigCount = 0;
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                count++;
                URL resource = resources.nextElement();
                String mixinConfigs = readMixinConfigs(resource);
                if (mixinConfigs != null) {
                    mixinConfigCount++;
                    SaltsInventoryUpdate.LOGGER.info(
                        "[forge-diagnostics] manifest with MixinConfigs #{} -> {} value={}",
                        mixinConfigCount,
                        resource,
                        mixinConfigs
                    );
                }
            }
        } catch (IOException exception) {
            SaltsInventoryUpdate.LOGGER.warn("[forge-diagnostics] manifest lookup failed", exception);
            return;
        }

        SaltsInventoryUpdate.LOGGER.info("[forge-diagnostics] manifests scanned={} manifestsWithMixinConfigs={}", count, mixinConfigCount);
        if (mixinConfigCount == 0) {
            SaltsInventoryUpdate.LOGGER.warn("[forge-diagnostics] no manifest advertised MixinConfigs=salts_inventory_update.mixins.json");
        }
    }

    private static String readMixinConfigs(URL resource) {
        try (InputStream input = resource.openStream()) {
            Manifest manifest = new Manifest(input);
            return manifest.getMainAttributes().getValue("MixinConfigs");
        } catch (IOException exception) {
            SaltsInventoryUpdate.LOGGER.warn("[forge-diagnostics] unable to read manifest {}", resource, exception);
            return null;
        }
    }

    private static ClassLoader contextLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : SaltsInventoryUpdateForge.class.getClassLoader();
    }
}
