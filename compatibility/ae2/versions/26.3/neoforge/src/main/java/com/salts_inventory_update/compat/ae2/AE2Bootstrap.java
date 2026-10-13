package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.compat.ae2.menu.MenuOpeningData;
import com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin;
import com.salts_inventory_update.compat.ae2.nativeview.AE2ClientIntegration;
import com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry;
import com.salts_inventory_update.compat.ae2.server.AE2ServerIntegration;

/** Defers optional AE2 contracts until native registry and screen registration has finished. */
public final class AE2Bootstrap {
    private static boolean commonRegistered;
    private static boolean clientRegistered;
    private AE2Bootstrap() { }

    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server -> registerCommon());
    }

    private static void registerCommon() {
        if (commonRegistered || !NativeDesktopPatchPlugin.active()) return;
        DesktopEnvironment.registerIntegration("ae2", AE2ServerIntegration::initialize);
        commonRegistered = true;
        MenuOpeningData.enableNativeDesktopOpening();
    }

    public static void initializeClient() {
        // FML client setup can run after the facade's startup callback. A native
        // client tick also guarantees menu-screen registration has completed.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> {
            if (clientRegistered || !NativeDesktopPatchPlugin.active()) return;
            DesktopEnvironment.registerIntegration("ae2", () -> {
                if (!commonRegistered) AE2ServerIntegration.initialize();
                AE2ClientIntegration.initialize();
            });
            commonRegistered = true;
            clientRegistered = true;
            MenuOpeningData.enableNativeDesktopOpening();
        });
    }
}
