package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.controlify.qa.ControlifyGameplayHarness;
import net.fabricmc.api.ClientModInitializer;

public final class QaClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("salts_inventory_controlify.qa")) ControlifyGameplayHarness.initialize();
    }
}
