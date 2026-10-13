package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.core.sync.BasePacket;
import com.salts_inventory_update.compat.ae2.client.DesktopContentRegistry;
import com.salts_inventory_update.compat.ae2.menu.DesktopMenuPackets;
import java.util.UUID;
import java.util.function.Consumer;

/** Native AE2 15 facade for the fork's lifetime-addressed update receivers. */
public final class NativeContentRegistry {
    private static final DesktopContentRegistry<BasePacket> RECEIVERS = new DesktopContentRegistry<>(DesktopMenuPackets::release);
    private NativeContentRegistry() { }
    public static void register(UUID owner, Consumer<BasePacket> receiver) {
        RECEIVERS.register(owner, packet -> {
            try { receiver.accept(packet); }
            finally { DesktopMenuPackets.release(packet); }
        });
    }
    public static void remove(UUID owner) { RECEIVERS.remove(owner); }
    public static boolean receive(UUID owner, BasePacket packet) { return RECEIVERS.receive(owner, packet); }
    public static void awaitOpening(UUID owner, BasePacket packet, int bytes) { RECEIVERS.awaitOpening(owner, packet, bytes); }
    public static void clear() { RECEIVERS.clear(); }
}
