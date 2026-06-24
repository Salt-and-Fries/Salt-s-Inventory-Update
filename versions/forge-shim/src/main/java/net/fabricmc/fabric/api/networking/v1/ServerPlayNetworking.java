package net.fabricmc.fabric.api.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class ServerPlayNetworking {
    private static final Map<ResourceLocation, PlayChannelHandler> RECEIVERS = new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    public static void registerGlobalReceiver(ResourceLocation id, PlayChannelHandler handler) {
        RECEIVERS.put(id, handler);
    }

    static void receive(ForgeNetworking.NetworkMessage message, NetworkEvent.Context context) {
        PlayChannelHandler handler = RECEIVERS.get(message.id());
        ServerPlayer player = context.getSender();
        if (handler != null && player != null) {
            handler.receive(player.server, player, player.connection, message.buffer(), PacketSender.INSTANCE);
        }
    }

    public static boolean canSend(ServerPlayer player, ResourceLocation id) {
        return player.connection != null && ForgeNetworking.CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static void send(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        ForgeNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ForgeNetworking.message(id, buf));
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
