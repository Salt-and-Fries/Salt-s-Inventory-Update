package net.fabricmc.fabric.api.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPlayNetworking {
    private static final Map<CustomPacketPayload.Type<?>, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type,
        PlayPayloadHandler<T> handler
    ) {
        RECEIVERS.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> void receive(T payload, IPayloadContext context) {
        PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) RECEIVERS.get(payload.type());
        if (handler != null) {
            handler.receive(payload, new Context(context));
        }
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public static final class Context {
        private final IPayloadContext context;

        private Context(IPayloadContext context) {
            this.context = context;
        }

        public MinecraftServer server() {
            return ((ServerPlayer) context.player()).level().getServer();
        }

        public ServerPlayer player() {
            return (ServerPlayer) context.player();
        }
    }
}
