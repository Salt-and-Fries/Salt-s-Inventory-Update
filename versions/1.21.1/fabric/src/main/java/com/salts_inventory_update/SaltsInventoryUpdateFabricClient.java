package com.salts_inventory_update;

import net.fabricmc.api.ClientModInitializer;

import com.salts_inventory_update.client.WindowedInventoryClient;

public final class SaltsInventoryUpdateFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SaltsInventoryUpdate.LOGGER.info("{} client initialized for Fabric {}", SaltsInventoryUpdate.MOD_NAME, VersionInfo.MINECRAFT_VERSION);
        WindowedInventoryClient.initialize();
    }
}
