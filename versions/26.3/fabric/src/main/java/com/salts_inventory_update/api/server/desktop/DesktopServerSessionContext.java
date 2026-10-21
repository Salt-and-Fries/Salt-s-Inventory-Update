package com.salts_inventory_update.api.server.desktop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopServerSessionContext<T extends AbstractContainerMenu, S> {
    ServerPlayer player();

    T menu();

    int sessionId();

    String sourceKey();

    boolean visible();



    S state();

    void sendToClient(Identifier channel, byte[] data);

    default <P> void sendToClient(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec) {
        this.sendToClient(channel, com.salts_inventory_update.api.desktop.DesktopPayloadCodecs.encode(this.player().registryAccess(), codec, payload));
    }

    /** Acquires once per parent lifetime and actual inventory identity. Empty means unavailable or unauthorized. */
    java.util.Optional<DesktopCompanionLease> acquireCompanion(DesktopCompanionSpec spec);

    /** Returns a copy of the authenticated player's shared desktop cursor.
     * Available only on the server thread while this session is alive. */
    default net.minecraft.world.item.ItemStack carriedStack() {
        throw new UnsupportedOperationException("Shared cursor access is not supported by this context");
    }

    /** Commits a native menu action's cursor to the desktop and clears detached
     * menu copies. Call broadcastChanges() after completing the action. */
    default void setCarriedStack(net.minecraft.world.item.ItemStack stack) {
        throw new UnsupportedOperationException("Shared cursor access is not supported by this context");
    }

    void broadcastChanges();
}
