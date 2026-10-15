package com.salts_inventory_update.controlify;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint for the independent controller compatibility mod. */
public final class SaltsControlifyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SaltsControlifyClient.initialize();
    }
}
