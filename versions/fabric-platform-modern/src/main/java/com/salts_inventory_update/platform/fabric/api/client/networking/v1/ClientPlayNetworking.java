package com.salts_inventory_update.platform.fabric.api.client.networking.v1;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPlayNetworking {
    private ClientPlayNetworking() {
    }

    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type,
        PlayPayloadHandler<T> handler
    ) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            type,
            (payload, context) -> handler.receive(payload, new Context(context.client()))
        );
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(type);
    }

    public static void send(CustomPacketPayload payload) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
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
