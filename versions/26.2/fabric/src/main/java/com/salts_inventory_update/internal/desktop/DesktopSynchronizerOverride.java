package com.salts_inventory_update.internal.desktop;

import java.util.function.Supplier;

import net.minecraft.world.inventory.ContainerSynchronizer;

/** Thread-confined handoff used while a third-party menu installs its synchronizer. */
public final class DesktopSynchronizerOverride {
    private static final ThreadLocal<ContainerSynchronizer> ACTIVE = new ThreadLocal<>();

    private DesktopSynchronizerOverride() {
    }

    public static <T> T call(ContainerSynchronizer synchronizer, Supplier<T> action) {
        ContainerSynchronizer previous = ACTIVE.get();
        ACTIVE.set(synchronizer);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }

    public static void run(ContainerSynchronizer synchronizer, Runnable action) {
        call(synchronizer, () -> {
            action.run();
            return null;
        });
    }

    public static ContainerSynchronizer preserveDesktop(ContainerSynchronizer proposed) {
        ContainerSynchronizer active = ACTIVE.get();
        return active instanceof DesktopSessionSynchronizer ? active : proposed;
    }
}
