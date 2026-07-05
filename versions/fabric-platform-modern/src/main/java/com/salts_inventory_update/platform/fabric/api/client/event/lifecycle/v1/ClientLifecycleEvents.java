package com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1;

import net.minecraft.client.Minecraft;

public final class ClientLifecycleEvents {
    public static final ClientStarted CLIENT_STARTED = new ClientStarted();

    private ClientLifecycleEvents() {
    }

    @FunctionalInterface
    public interface ClientStartedHandler {
        void onClientStarted(Minecraft client);
    }

    public static final class ClientStarted {
        public void register(ClientStartedHandler handler) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(handler::onClientStarted);
        }
    }
}
