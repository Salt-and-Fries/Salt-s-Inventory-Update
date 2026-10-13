package com.salts_inventory_update.compat.ae2.nativeview;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class LegacyLifecycle {
    private LegacyLifecycle() { }
    public static void onClientTick(Runnable callback) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase==TickEvent.Phase.END) callback.run();
        });
    }
    public static void onDisconnect(Runnable callback) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> callback.run());
    }
}
