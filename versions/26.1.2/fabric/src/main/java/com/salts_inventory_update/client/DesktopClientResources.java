package com.salts_inventory_update.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Client resource listener without loader-specific APIs. */
final class DesktopClientResources {
    private DesktopClientResources() { }
    static void install(Minecraft client) {
        // NeoForge installs this listener through its early mod-bus reload event.
        if (com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("neoforge")) return;
        if (client.getResourceManager() instanceof ReloadableResourceManager resources) {
            resources.registerReloadListener((ResourceManagerReloadListener) manager ->
                client.execute(InventoryDesktopScreen::resourcesReloaded));
        }
    }
}
