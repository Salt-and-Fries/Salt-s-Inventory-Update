package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

public final class ClientTickEvents {
    public static final Tick START_CLIENT_TICK = new Tick();
    public static final Tick END_CLIENT_TICK = new Tick();

    private ClientTickEvents() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            START_CLIENT_TICK.invoker(Minecraft.getInstance());
        } else if (event.phase == TickEvent.Phase.END) {
            END_CLIENT_TICK.invoker(Minecraft.getInstance());
        }
    }

    @FunctionalInterface
    public interface TickHandler {
        void onTick(Minecraft client);
    }

    public static final class Tick {
        private final List<TickHandler> handlers = new ArrayList<>();

        public void register(TickHandler handler) {
            handlers.add(handler);
        }

        private void invoker(Minecraft client) {
            for (TickHandler callback : handlers) {
                callback.onTick(client);
            }
        }
    }
}
