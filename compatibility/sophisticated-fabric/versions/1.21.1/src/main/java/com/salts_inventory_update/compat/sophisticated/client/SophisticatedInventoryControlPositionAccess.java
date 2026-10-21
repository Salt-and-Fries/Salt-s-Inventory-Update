package com.salts_inventory_update.compat.sophisticated.client;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

/** Internal position bridge for the two native inventory-column controls supplied by Sophisticated Core. */
public interface SophisticatedInventoryControlPositionAccess {
    Position salts_inventory_update$getPosition();

    void salts_inventory_update$setPosition(Position position);

    int salts_inventory_update$getHeight();
}
