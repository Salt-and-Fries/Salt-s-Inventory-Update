package com.salts_inventory_update.platform.fabric.api.networking.v1;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayNetworking {
    private ServerPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type,
        PlayPayloadHandler<T> handler
    ) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            type,
            (payload, context) -> handler.receive(payload, new Context(context.server(), context.player()))
        );
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, type);
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public static final class Context {
        private final MinecraftServer server;
        private final ServerPlayer player;

        private Context(MinecraftServer server, ServerPlayer player) {
            this.server = server;
            this.player = player;
        }

        public MinecraftServer server() {
            return server;
        }

        public ServerPlayer player() {
            return player;
        }
    }
}
