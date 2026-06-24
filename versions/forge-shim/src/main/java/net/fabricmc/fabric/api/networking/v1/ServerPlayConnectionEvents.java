package net.fabricmc.fabric.api.networking.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class ServerPlayConnectionEvents {
    public static final Disconnect DISCONNECT = new Disconnect();

    private ServerPlayConnectionEvents() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.connection != null) {
            DISCONNECT.invoker(player.connection, player.server);
        }
    }

    @FunctionalInterface
    public interface DisconnectHandler {
        void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server);
    }

    public static final class Disconnect {
        private final List<DisconnectHandler> handlers = new ArrayList<>();

        public void register(DisconnectHandler handler) {
            handlers.add(handler);
        }

        private void invoker(ServerGamePacketListenerImpl handler, MinecraftServer server) {
            for (DisconnectHandler callback : handlers) {
                callback.onPlayDisconnect(handler, server);
            }
        }
    }
}
