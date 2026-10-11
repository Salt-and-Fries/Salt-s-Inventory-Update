package com.salts_inventory_update.api.server.desktop;

/** Server-thread ownership reference for one parent lifetime and one shared inventory companion. */
public interface DesktopCompanionLease extends AutoCloseable {
    /** Salt's authenticated inventory session, independent of any one parent. */
    int sessionId();
    boolean isOpen();
    /** Releases only this parent association. Repeated calls are harmless. */
    @Override void close();
}
