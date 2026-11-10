package com.salts_inventory_update.api.client.desktop;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.ResourceLocation;

/** Client-thread callbacks for an opt-in menu with no independent window. */
public interface DesktopEmbeddedSessionHandler<T extends AbstractContainerMenu> {
    /** False rejects an unsolicited/ownerless session and requests its normal close. */
    boolean opened(DesktopEmbeddedSession<T> session);
    /** Exactly once when Core disposes the native client session. */
    default void closed(int sessionId) { }
    default void customPayload(DesktopEmbeddedSession<T> session, ResourceLocation channel, byte[] data) { }
}
