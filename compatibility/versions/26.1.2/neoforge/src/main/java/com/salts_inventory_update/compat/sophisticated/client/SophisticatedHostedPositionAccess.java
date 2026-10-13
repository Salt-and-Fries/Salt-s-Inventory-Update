package com.salts_inventory_update.compat.sophisticated.client;

/**
 * Internal hook for native Sophisticated widgets that cache absolute child geometry outside the normal
 * {@code CompositeWidgetBase} tree.
 */
public interface SophisticatedHostedPositionAccess {
    void salts_inventory_update$translateHostedPosition(int dx, int dy);
}
