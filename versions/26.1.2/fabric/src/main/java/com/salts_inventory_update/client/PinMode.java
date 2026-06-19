package com.salts_inventory_update.client;

enum PinMode {
    UNPINNED,
    PINNED,
    GHOST_PINNED;

    PinMode next() {
        return switch (this) {
            case UNPINNED -> PINNED;
            case PINNED -> GHOST_PINNED;
            case GHOST_PINNED -> UNPINNED;
        };
    }
}
