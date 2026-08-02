package com.salts_inventory_update.api.server.desktop;

import java.util.Objects;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Server-authenticated recipe transfer request. The recipe instance comes from
 * the server recipe manager; client-provided ingredient lists are not exposed.
 */
public record DesktopTransferRequest<T extends AbstractContainerMenu, S>(
    DesktopServerSessionContext<T, S> session,
    Recipe<?> recipe,
    boolean maxTransfer
) {
    public DesktopTransferRequest {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(recipe, "recipe");
    }
}

