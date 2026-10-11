package com.salts_inventory_update.api.server.desktop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerPayloadContext<T extends AbstractContainerMenu> extends DesktopServerSessionContext<T, Object> {
    ResourceLocation channel();

    byte[] data();

    /** Reports a mod-specific action refusal without granting any mutation privilege. */
    default void reject(String reason) {
        java.util.Objects.requireNonNull(reason, "reason");
        if (reason.length() > 512) throw new IllegalArgumentException("Action rejection reason is too long");
        com.salts_inventory_update.api.integration.DesktopDiagnostics.report(
            com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.WARNING,
            this.sourceKey(), "custom-action:" + this.channel(), reason, null);
    }
}
