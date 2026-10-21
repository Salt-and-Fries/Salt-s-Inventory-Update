package com.salts_inventory_update.compat.sophisticated;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.compat.CompatibilityBootstrap;

/** Waits for Fabric's native registry and screen initializers before installing each optional adapter. */
public final class SophisticatedFabricBootstrap {
    private static boolean commonScheduled, commonRegistered, clientScheduled, clientRegistered;
    private SophisticatedFabricBootstrap() { }
    public static synchronized void initializeCommon() {
        if (commonScheduled) return;
        commonScheduled = true;
        DesktopEnvironment.onServerStarting(server -> registerCommon());
    }
    public static synchronized void initializeClient() {
        if (clientScheduled) return;
        clientScheduled = true;
        DesktopClientApi.onClientStarted(client -> {
            registerCommon();
            if (clientRegistered) return;
            register("Client");
            clientRegistered = true;
        });
    }
    private static synchronized void registerCommon() {
        if (commonRegistered) return;
        register("Common");
        commonRegistered = true;
    }
    private static void register(String side) {
        CompatibilityBootstrap.initialize("sophisticatedcore", "com.salts_inventory_update.compat.sophisticated.SophisticatedCoreCompat", "initialize" + side);
        CompatibilityBootstrap.initialize("sophisticatedbackpacks", "com.salts_inventory_update.compat.sophisticated.SophisticatedBackpacksCompat", "initialize" + side);
        CompatibilityBootstrap.initialize("sophisticatedstorage", "com.salts_inventory_update.compat.sophisticated.SophisticatedStorageCompat", "initialize" + side);
    }
}
