package com.salts_inventory_update.api.server.desktop;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerWindowHandler<T extends AbstractContainerMenu, S> {
    /** Pure per-instance eligibility check before capture; false leaves the normal screen in charge. */
    default boolean supports(net.minecraft.server.level.ServerPlayer player, T menu) { return true; }

    /** Route player inventory quick-move through this menu (e.g. virtual storage). */
    default boolean usesNativePlayerInventoryQuickMove(T menu) { return false; }

    default @Nullable S createState(DesktopServerSessionContext<T, S> context) {
        return null;
    }

    default void opened(DesktopServerSessionContext<T, S> context) {
    }

    default void tick(DesktopServerSessionContext<T, S> context) {
    }

    default void closed(DesktopServerSessionContext<T, S> context) {
    }

    default void visibilityChanged(DesktopServerSessionContext<T, S> context, boolean visible) {
    }



    /**
     * Validates a recipe-browser transfer for a custom menu using a recipe
     * resolved by the server. Transfers stay disabled until the handler explicitly allows one.
     */
    default DesktopTransferDecision validateTransfer(DesktopTransferRequest<T, S> request) {
        return DesktopTransferDecision.unsupported();
    }
}
