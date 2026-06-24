package net.fabricmc.fabric.api.client.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPlayNetworking {
    private static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

    private ClientPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type,
        PlayPayloadHandler<T> handler
    ) {
        RECEIVERS.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void receive(T payload, IPayloadContext ignored) {
        PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) RECEIVERS.get(payload.type());
        if (handler != null) {
            handler.receive(payload, new Context(Minecraft.getInstance()));
        }
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && listener.hasChannel(type);
    }

    public static void send(CustomPacketPayload payload) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null || !listener.hasChannel(payload.type())) {
            throw new IllegalStateException("Cannot send payload without a negotiated channel: " + payload.type().id());
        }
        listener.send(payload);
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public static final class Context {
        private final Minecraft client;

        private Context(Minecraft client) {
            this.client = client;
        }

        public Minecraft client() {
            return client;
        }
    }
}
