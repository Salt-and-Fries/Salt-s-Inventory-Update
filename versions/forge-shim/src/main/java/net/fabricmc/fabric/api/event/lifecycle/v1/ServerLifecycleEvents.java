package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartingEvent;

public final class ServerLifecycleEvents {
    public static final ServerStarting SERVER_STARTING = new ServerStarting();

    private ServerLifecycleEvents() {
    }

    public static void onServerStarting(ServerStartingEvent event) {
        SERVER_STARTING.invoker(event.getServer());
    }

    @FunctionalInterface
    public interface ServerStartingHandler {
        void onServerStarting(MinecraftServer server);
    }

    public static final class ServerStarting {
        private final List<ServerStartingHandler> handlers = new ArrayList<>();

        public void register(ServerStartingHandler handler) {
            handlers.add(handler);
        }

        private void invoker(MinecraftServer server) {
            for (ServerStartingHandler callback : handlers) {
                callback.onServerStarting(server);
            }
        }
    }
}
