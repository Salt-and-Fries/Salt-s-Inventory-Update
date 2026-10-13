package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.core.network.ClientboundPacket;
import com.salts_inventory_update.compat.ae2.client.DesktopContentRegistry;
import java.util.UUID;
import java.util.function.Consumer;

/** Typed native facade for the fork's lifetime receiver contract. */
public final class NativeContentRegistry {
    private static final DesktopContentRegistry<ClientboundPacket> RECEIVERS = new DesktopContentRegistry<>();
    private NativeContentRegistry() { }
    public static void register(UUID owner, Consumer<ClientboundPacket> receiver) { RECEIVERS.register(owner, receiver); }
    public static void remove(UUID owner) { RECEIVERS.remove(owner); }
    public static boolean receive(UUID owner, ClientboundPacket packet) { return RECEIVERS.receive(owner, packet); }
    public static void awaitOpening(UUID owner, ClientboundPacket packet, int bytes) { RECEIVERS.awaitOpening(owner, packet, bytes); }
    public static void clear() { RECEIVERS.clear(); }
}
