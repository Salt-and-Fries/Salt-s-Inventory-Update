package com.salts_inventory_update.platform.fabric.api.client.command.v2;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface ClientCommandRegistrationCallback {
    Event EVENT = new Event();

    @SuppressWarnings("rawtypes")
    void register(CommandDispatcher dispatcher, Object registryAccess);

    final class Event {
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void register(ClientCommandRegistrationCallback callback) {
            net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> callback.register((CommandDispatcher) dispatcher, registryAccess)
            );
        }
    }
}
