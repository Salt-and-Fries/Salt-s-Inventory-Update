package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.fabricmc.api.ClientModInitializer;

/** Entrypoint of the separate, opt-in QA mod, never the distributable companion. */
public final class ControlifyQaInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("salts_inventory_controlify.qa")) {
            ControlifyGameplayHarness.initialize();
        }
    }
}
