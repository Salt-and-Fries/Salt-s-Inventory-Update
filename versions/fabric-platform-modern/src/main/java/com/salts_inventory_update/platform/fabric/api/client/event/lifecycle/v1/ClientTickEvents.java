package com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1;

import net.minecraft.client.Minecraft;

public final class ClientTickEvents {
    public static final Tick START_CLIENT_TICK = new Tick(true);
    public static final Tick END_CLIENT_TICK = new Tick(false);

    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface TickHandler {
        void onTick(Minecraft client);
    }

    public static final class Tick {
        private final boolean start;

        private Tick(boolean start) {
            this.start = start;
        }

        public void register(TickHandler handler) {
            if (start) {
                net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(handler::onTick);
            } else {
                net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(handler::onTick);
            }
        }
    }
}
