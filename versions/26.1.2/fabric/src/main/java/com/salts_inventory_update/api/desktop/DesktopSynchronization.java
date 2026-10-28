package com.salts_inventory_update.api.desktop;

import net.minecraft.world.inventory.ContainerSynchronizer;

/** Allows a native menu to retain desktop synchronization while it installs its own synchronizer. */
public final class DesktopSynchronization {
    private DesktopSynchronization() { }
    public static ContainerSynchronizer preserveDesktop(ContainerSynchronizer proposed) {
        return com.salts_inventory_update.internal.desktop.DesktopSynchronizerOverride.preserveDesktop(proposed);
    }
    /** Whether a synchronizer carries an authenticated desktop session. Native menu
     * adapters can route it through their normal full-count synchronization setter.
     * This does not transfer ownership or expose Core implementation types. */
    public static boolean isDesktopSynchronizer(ContainerSynchronizer synchronizer) {
        return synchronizer instanceof com.salts_inventory_update.internal.desktop.DesktopSessionSynchronizer;
    }
}
