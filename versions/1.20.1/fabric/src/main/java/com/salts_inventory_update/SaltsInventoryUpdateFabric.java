package com.salts_inventory_update;

import net.fabricmc.api.ModInitializer;

public final class SaltsInventoryUpdateFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SaltsInventoryUpdate.init("Fabric " + VersionInfo.MINECRAFT_VERSION);
    }
}
