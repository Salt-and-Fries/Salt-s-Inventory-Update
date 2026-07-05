package com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1;

import net.minecraft.server.MinecraftServer;

public final class ServerTickEvents {
    public static final EndTick END_SERVER_TICK = new EndTick();

    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface EndTickHandler {
        void onEndTick(MinecraftServer server);
    }

    public static final class EndTick {
        public void register(EndTickHandler handler) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(handler::onEndTick);
        }
    }
}
