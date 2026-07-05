package com.salts_inventory_update.platform.fabric.api.networking.v1;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class ServerPlayConnectionEvents {
    public static final Disconnect DISCONNECT = new Disconnect();

    private ServerPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface DisconnectHandler {
        void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server);
    }

    public static final class Disconnect {
        public void register(DisconnectHandler handler) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(handler::onPlayDisconnect);
        }
    }
}
