package com.salts_inventory_update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaltsInventoryUpdate {
    public static final String MOD_ID = "salts_inventory_update";
    public static final String MOD_NAME = "Salt's Inventory Update";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    static {
        com.salts_inventory_update.api.integration.DesktopDiagnostics.listen(event -> {
            String detail = "[desktop-api] " + event.owner() + " " + event.operation() + ": " + event.message();
            switch (event.level()) {
                case INFO -> LOGGER.info(detail);
                case WARNING -> LOGGER.warn(detail, event.cause());
                case ERROR -> LOGGER.error(detail, event.cause());
            }
        });
    }

    private SaltsInventoryUpdate() {
    }

    public static void init(String target) {
        LOGGER.info("{} initialized for {}", MOD_NAME, target);
    }
}
