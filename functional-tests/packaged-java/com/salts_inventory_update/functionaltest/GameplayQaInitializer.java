package com.salts_inventory_update.functionaltest;

/** Entry point of the separate, opt-in QA mod used for packaged artifact launches. */
public final class GameplayQaInitializer implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        InWorldGameplayHarness.initialize();
    }
}
