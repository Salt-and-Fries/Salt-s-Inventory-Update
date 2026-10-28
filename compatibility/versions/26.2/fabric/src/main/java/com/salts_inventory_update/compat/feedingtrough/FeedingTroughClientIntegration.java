package com.salts_inventory_update.compat.feedingtrough;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Compact one-slot desktop view; Core supplies chrome, tooltips and inventory gestures. */
public final class FeedingTroughClientIntegration {
    private static boolean lifecycleHook;
    private static boolean registered;
    private FeedingTroughClientIntegration() { }

    public static synchronized void initialize() {
        if (!DesktopEnvironment.isModLoaded(FeedingTroughIntegration.MOD_ID)) return;
        register();
        if (!lifecycleHook) {
            lifecycleHook = true;
            DesktopClientApi.onClientStarted(client -> register());
        }
    }

    private static synchronized void register() {
        if (registered) return;
        var type = FeedingTroughIntegration.menuType();
        if (type == null) return;
        DesktopEnvironment.registerIntegration(FeedingTroughIntegration.MOD_ID, () -> {
            FeedingTroughIntegration.registerStorage(type);
            DesktopClientApi.registerWindow(type, new TroughWindow());
        });
        registered = true;
    }

    private static final class TroughWindow implements DesktopWindowDefinition<AbstractContainerMenu, Void> {
        // One 18px row plus Salt's existing 24px header and 8px bottom inset.
        private static final DesktopWindowSize SIZE = DesktopWindowSize.of(200, 50);
        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> c) { return SIZE; }
        @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu, Void> c) { return SIZE; }
        @Override public DesktopWindowSize maxSize(DesktopWindowContext<AbstractContainerMenu, Void> c) { return SIZE; }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu, Void> c) { return "feeding-trough-1"; }

        private int slotX(DesktopWindowContext<AbstractContainerMenu, Void> c) { return c.contentX() + (c.contentWidth() - 18) / 2 + 1; }
        private int slotY(DesktopWindowContext<AbstractContainerMenu, Void> c) { return c.contentY() + (c.contentHeight() - 18) / 2 + 1; }

        @Override public void render(DesktopRenderContext<AbstractContainerMenu, Void> c) {
            c.slot(c.containerSlot(0), slotX(c), slotY(c));
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> c, double x, double y) {
            return c.containerSlotHit(0, slotX(c), slotY(c), x, y);
        }
    }
}
