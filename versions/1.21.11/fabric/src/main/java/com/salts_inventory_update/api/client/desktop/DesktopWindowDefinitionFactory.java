package com.salts_inventory_update.api.client.desktop;

@FunctionalInterface
public interface DesktopWindowDefinitionFactory {
    DesktopWindowDefinition<?, ?> create(DesktopWindowLookupContext context);
}
