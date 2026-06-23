package com.salts_inventory_update.api.client.desktop;

@FunctionalInterface
public interface DesktopWindowMatcher {
    boolean matches(DesktopWindowLookupContext context);
}
