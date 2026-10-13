package com.salts_inventory_update.compat;
import net.fabricmc.api.ClientModInitializer;
public final class SaltsInventoryCompatibilityFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() { CompatibilityBootstrap.initializeClient(); }
}
