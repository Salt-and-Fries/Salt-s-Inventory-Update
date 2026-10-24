package com.salts_inventory_update.functionaltest;

/** Entry point of the separate, opt-in QA mod used for packaged artifact launches. */
public final class GameplayQaInitializer implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_CONFIG_MENU"))) {
            ConfigMenuQa.initialize();
            return;
        }
        if (System.getenv("SIU_QA_SPATIAL_SERVER") != null) {
            try { Class.forName("com.salts_inventory_update.functionaltest.SpatialServerClientQa").getMethod("initialize").invoke(null); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        } else InWorldGameplayHarness.initialize();
    }
}
