package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerTickEvents {
    public static final EndTick END_SERVER_TICK = new EndTick();

    private ServerTickEvents() {
    }

    public static void onEndServerTick(ServerTickEvent.Post event) {
        END_SERVER_TICK.invoker(event.getServer());
    }

    @FunctionalInterface
    public interface EndTickHandler {
        void onEndTick(MinecraftServer server);
    }

    public static final class EndTick {
        private final List<EndTickHandler> handlers = new ArrayList<>();

        public void register(EndTickHandler handler) {
            handlers.add(handler);
        }

        private void invoker(MinecraftServer server) {
            for (EndTickHandler callback : handlers) {
                callback.onEndTick(server);
            }
        }
    }
}
