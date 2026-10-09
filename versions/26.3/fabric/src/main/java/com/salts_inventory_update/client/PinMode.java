package com.salts_inventory_update.client;

enum PinMode {
    UNPINNED,
    PINNED,
    GHOST_PINNED;

    PinMode next(boolean enableGhostPins) {
        return switch (this) {
            case UNPINNED -> PINNED;
            case PINNED -> enableGhostPins ? GHOST_PINNED : UNPINNED;
            case GHOST_PINNED -> UNPINNED;
        };
    }
}
