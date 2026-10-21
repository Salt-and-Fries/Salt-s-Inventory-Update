package com.salts_inventory_update.client;

enum PinMode {
    UNPINNED,
    PINNED;

    PinMode next() {
        return this == UNPINNED ? PINNED : UNPINNED;
    }
}
