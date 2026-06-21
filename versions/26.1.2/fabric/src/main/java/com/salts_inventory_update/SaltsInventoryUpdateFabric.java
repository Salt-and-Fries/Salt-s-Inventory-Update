package com.salts_inventory_update;

import net.fabricmc.api.ModInitializer;

import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat;
import com.salts_inventory_update.server.DesktopContainerSessions;

public final class SaltsInventoryUpdateFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SaltsInventoryUpdate.init("Fabric " + VersionInfo.MINECRAFT_VERSION);
        DesktopPackets.registerPayloadTypes();
        DesktopContainerSessions.initialize();
        TomsStorageServerCompat.initialize();
    }
}
