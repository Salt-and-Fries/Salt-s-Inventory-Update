package com.salts_inventory_update.api.integration;

/**
 * A lazily loaded integration provider. Declare its class name in
 * {@code META-INF/salts_inventory_update.integration.properties}; do not instantiate it
 * from your mod's ordinary entrypoint. Common and client providers use separate classes.
 */
@FunctionalInterface
public interface DesktopIntegration {
    void register(DesktopIntegrationContext context);
}
