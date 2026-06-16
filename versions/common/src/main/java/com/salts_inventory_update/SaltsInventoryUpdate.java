package com.salts_inventory_update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaltsInventoryUpdate {
    public static final String MOD_ID = "salts_inventory_update";
    public static final String MOD_NAME = "Salt's Inventory Update";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private SaltsInventoryUpdate() {
    }

    public static void init(String target) {
        LOGGER.info("{} initialized for {}", MOD_NAME, target);
    }
}
