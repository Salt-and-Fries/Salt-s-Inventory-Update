package com.salts_inventory_update.api.integration;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/** Loader-neutral integration lifecycle. Client hooks live in DesktopClientApi. */
public final class DesktopEnvironment {
    private DesktopEnvironment() { }
    /** Whether the user's detailed console logging or development diagnostics are enabled. */
    public static boolean diagnosticsEnabled() { return com.salts_inventory_update.debug.DesktopDebug.enabled(); }
    public static boolean traceDiagnosticsEnabled() { return com.salts_inventory_update.debug.DesktopDebug.traceEnabled(); }
    public static boolean isModLoaded(String id) {
        return com.salts_inventory_update.platform.loader.api.FabricLoader.getInstance().isModLoaded(id);
    }
    public static void onServerStarting(Consumer<MinecraftServer> action) {
        com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(action::accept);
    }
    /** Atomically registers one integration; a failed integration cannot undo another's registrations. */
    public static void registerIntegration(String owner, Runnable action) {
        com.salts_inventory_update.integration.DesktopRegistrationScope.run(owner, action);
    }
}
