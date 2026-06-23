package com.salts_inventory_update.api.server.desktop;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerWindowHandler<T extends AbstractContainerMenu, S> {
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

    default void pinChanged(DesktopServerSessionContext<T, S> context, boolean ghostPinned) {
    }
}
