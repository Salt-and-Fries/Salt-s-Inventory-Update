package com.salts_inventory_update.platform.fabric.api.networking.v1;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class ServerPlayNetworking {
    private ServerPlayNetworking() {
    }

    public static void registerGlobalReceiver(ResourceLocation id, PlayChannelHandler handler) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            id,
            (server, player, networkHandler, buf, sender) -> handler.receive(server, player, networkHandler, buf, PacketSender.INSTANCE)
        );
    }

    public static boolean canSend(ServerPlayer player, ResourceLocation id) {
        return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, id);
    }

    public static void send(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, id, buf);
    }

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(
            MinecraftServer server,
            ServerPlayer player,
            ServerGamePacketListenerImpl handler,
            FriendlyByteBuf buf,
            PacketSender responseSender
        );
    }

    public static final class PacketSender {
        private static final PacketSender INSTANCE = new PacketSender();

        private PacketSender() {
        }
    }
}
