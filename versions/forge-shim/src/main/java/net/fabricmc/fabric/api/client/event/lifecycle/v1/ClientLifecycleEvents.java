package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientLifecycleEvents {
    public static final ClientStarted CLIENT_STARTED = new ClientStarted();

    private ClientLifecycleEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CLIENT_STARTED.invoker(Minecraft.getInstance()));
    }

    @FunctionalInterface
    public interface ClientStartedHandler {
        void onClientStarted(Minecraft client);
    }

    public static final class ClientStarted {
        private final List<ClientStartedHandler> handlers = new ArrayList<>();

        public void register(ClientStartedHandler handler) {
            handlers.add(handler);
        }

        private void invoker(Minecraft client) {
            for (ClientStartedHandler callback : handlers) {
                callback.onClientStarted(client);
            }
        }
    }
}
