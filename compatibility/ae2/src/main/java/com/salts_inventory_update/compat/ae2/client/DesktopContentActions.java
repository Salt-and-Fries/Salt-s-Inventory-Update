package com.salts_inventory_update.compat.ae2.client;

/** The fork's per-input callbacks, with native screen and click types supplied by each adapter. */
public interface DesktopContentActions<C, A> {
    boolean clickSlot(int slot, int button, A action);
    void close();
    void switchContent(C content);
}
