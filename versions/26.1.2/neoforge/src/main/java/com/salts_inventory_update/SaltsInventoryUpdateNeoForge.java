package com.salts_inventory_update;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SaltsInventoryUpdate.MOD_ID)
public final class SaltsInventoryUpdateNeoForge {
    public SaltsInventoryUpdateNeoForge(IEventBus modBus) {
        SaltsInventoryUpdate.init("NeoForge " + VersionInfo.MINECRAFT_VERSION);
    }
}
