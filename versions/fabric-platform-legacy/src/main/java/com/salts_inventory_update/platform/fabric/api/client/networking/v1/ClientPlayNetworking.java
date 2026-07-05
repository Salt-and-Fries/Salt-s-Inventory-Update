package com.salts_inventory_update.platform.fabric.api.client.networking.v1;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ClientPlayNetworking {
    private ClientPlayNetworking() {
    }

    public static void registerGlobalReceiver(ResourceLocation id, PlayChannelHandler handler) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            id,
            (client, listener, buf, sender) -> handler.receive(client, listener, buf, PacketSender.INSTANCE)
        );
    }

    public static boolean canSend(ResourceLocation id) {
        return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(id);
    }

    public static void send(ResourceLocation id, FriendlyByteBuf buf) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(id, buf);
    }

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(Minecraft client, ClientPacketListener listener, FriendlyByteBuf buf, PacketSender responseSender);
    }

    public static final class PacketSender {
        private static final PacketSender INSTANCE = new PacketSender();

        private PacketSender() {
        }
    }
}
