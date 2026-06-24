package com.salts_inventory_update;

import com.salts_inventory_update.compat.toms_storage.server.TomsStorageServerCompat;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.platform.ForgePlatform;
import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(SaltsInventoryUpdate.MOD_ID)
public final class SaltsInventoryUpdateForge {
    public SaltsInventoryUpdateForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgePlatform.initialize(modBus);

        SaltsInventoryUpdate.init("Forge " + VersionInfo.MINECRAFT_VERSION);
        DesktopPackets.registerPayloadTypes();
        DesktopContainerSessions.initialize();
        TomsStorageServerCompat.initialize();

        ForgePlatform.initializeClient(modBus);
    }
}
