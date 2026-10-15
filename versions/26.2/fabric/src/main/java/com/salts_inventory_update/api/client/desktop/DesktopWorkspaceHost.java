package com.salts_inventory_update.api.client.desktop;

/** Optional replacement presentation. No layouts, device inputs or animations belong in Core. */
public interface DesktopWorkspaceHost {
    boolean enabled();
    /** Invoked on a normal workspace opening, including additions while another screen is active. */
    void opened(DesktopWorkspace workspace);
}
