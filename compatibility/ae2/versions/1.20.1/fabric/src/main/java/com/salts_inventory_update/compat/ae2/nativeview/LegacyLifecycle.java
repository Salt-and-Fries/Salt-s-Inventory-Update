package com.salts_inventory_update.compat.ae2.nativeview;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class LegacyLifecycle {
    private LegacyLifecycle() { }
    public static void onClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }
    public static void onDisconnect(Runnable callback) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((connection,client) -> callback.run());
    }
}
