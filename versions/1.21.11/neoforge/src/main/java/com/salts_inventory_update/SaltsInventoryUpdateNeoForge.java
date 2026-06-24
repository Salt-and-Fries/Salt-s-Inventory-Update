package com.salts_inventory_update;

import com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.platform.NeoForgePlatform;
import com.salts_inventory_update.server.DesktopContainerSessions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SaltsInventoryUpdate.MOD_ID)
public final class SaltsInventoryUpdateNeoForge {
    public SaltsInventoryUpdateNeoForge(IEventBus modBus) {
        NeoForgePlatform.initialize(modBus);

        SaltsInventoryUpdate.init("NeoForge " + VersionInfo.MINECRAFT_VERSION);
        DesktopPackets.registerPayloadTypes();
        DesktopContainerSessions.initialize();
        TomsStorageServerCompat.initialize();

        NeoForgePlatform.initializeClient(modBus);
    }
}
