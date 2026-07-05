package com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1;

import net.minecraft.server.MinecraftServer;

public final class ServerLifecycleEvents {
    public static final ServerStarting SERVER_STARTING = new ServerStarting();

    private ServerLifecycleEvents() {
    }

    @FunctionalInterface
    public interface ServerStartingHandler {
        void onServerStarting(MinecraftServer server);
    }

    public static final class ServerStarting {
        public void register(ServerStartingHandler handler) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(handler::onServerStarting);
        }
    }
}
