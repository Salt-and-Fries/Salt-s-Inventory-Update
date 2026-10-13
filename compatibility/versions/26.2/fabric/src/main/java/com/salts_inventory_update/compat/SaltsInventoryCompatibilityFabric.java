package com.salts_inventory_update.compat;
import net.fabricmc.api.ModInitializer;
public final class SaltsInventoryCompatibilityFabric implements ModInitializer {
    @Override public void onInitialize() { CompatibilityBootstrap.initializeCommon(); }
}
