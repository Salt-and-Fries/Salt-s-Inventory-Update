package com.salts_inventory_update.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Client resource listener without loader-specific APIs. */
final class DesktopClientResources {
    private DesktopClientResources() { }
    static void install(Minecraft client) {
        if (client.getResourceManager() instanceof ReloadableResourceManager resources) {
            resources.registerReloadListener((ResourceManagerReloadListener) manager ->
                client.execute(InventoryDesktopScreen::resourcesReloaded));
        }
    }
}
