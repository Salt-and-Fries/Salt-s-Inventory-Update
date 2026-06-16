package com.salts_inventory_update;

import net.minecraftforge.fml.common.Mod;

@Mod(SaltsInventoryUpdate.MOD_ID)
public final class SaltsInventoryUpdateForge {
    public SaltsInventoryUpdateForge() {
        SaltsInventoryUpdate.init("Forge " + VersionInfo.MINECRAFT_VERSION);
    }
}
