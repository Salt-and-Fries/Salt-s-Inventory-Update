package com.salts_inventory_update.api.client.desktop;

/**
 * Adapts the preceding provider's immutable layout on the client thread. Called afresh for drawing
 * and input: read live state, return deterministically, and do not mutate game state in this callback.
 */
@FunctionalInterface
public interface DesktopHudLayoutProvider {
    DesktopHudLayout adapt(DesktopHudContext context, DesktopHudLayout layout);
}
