package com.salts_inventory_update.api.desktop;

import net.minecraft.world.inventory.ContainerSynchronizer;

/** Allows a native menu to retain desktop synchronization while it installs its own synchronizer. */
public final class DesktopSynchronization {
    private DesktopSynchronization() { }
    public static ContainerSynchronizer preserveDesktop(ContainerSynchronizer proposed) {
        return com.salts_inventory_update.internal.desktop.DesktopSynchronizerOverride.preserveDesktop(proposed);
    }
}
