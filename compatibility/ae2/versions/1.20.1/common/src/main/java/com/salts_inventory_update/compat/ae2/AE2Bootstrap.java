package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin;
import com.salts_inventory_update.compat.ae2.nativeview.AE2ClientIntegration;
import com.salts_inventory_update.compat.ae2.nativeview.LegacyLifecycle;
import com.salts_inventory_update.compat.ae2.menu.DesktopUpdateNetwork;
import com.salts_inventory_update.compat.ae2.server.AE2ServerIntegration;

/** Delays optional registration until native registries and screen factories are ready. */
public final class AE2Bootstrap {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(AE2Bootstrap.class);
    private static boolean commonInstalled, commonRegistered, clientRegistered;
    private AE2Bootstrap() { }
    public static void initializeCommon() {
        LOG.info("Initializing optional AE2 common hooks; native contract active={}",NativeDesktopPatchPlugin.active());
        if (commonInstalled||!NativeDesktopPatchPlugin.active()) return;
        DesktopUpdateNetwork.initializeCommon();
        DesktopEnvironment.onServerStarting(server -> registerCommon());
        commonInstalled=true;
    }
    private static void registerCommon() {
        if (commonRegistered||!NativeDesktopPatchPlugin.active()) return;
        DesktopEnvironment.registerIntegration("ae2",AE2ServerIntegration::initialize);
        LOG.info("Registered AE2 server desktop contracts");
        commonRegistered=true;
    }
    public static void initializeClient() {
        LOG.info("Initializing optional AE2 client hooks; native contract active={}",NativeDesktopPatchPlugin.active());
        if (!NativeDesktopPatchPlugin.active()) return;
        initializeCommon();
        DesktopUpdateNetwork.initializeClient();
        LegacyLifecycle.onDisconnect(com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry::clear);
        LegacyLifecycle.onClientTick(() -> {
            if (clientRegistered||!NativeDesktopPatchPlugin.active()) return;
            DesktopEnvironment.registerIntegration("ae2",() -> {
                if (!commonRegistered) AE2ServerIntegration.initialize();
                AE2ClientIntegration.initialize();
            });
            commonRegistered=true;clientRegistered=true;
            LOG.info("Registered AE2 client desktop contracts");
        });
    }
}
