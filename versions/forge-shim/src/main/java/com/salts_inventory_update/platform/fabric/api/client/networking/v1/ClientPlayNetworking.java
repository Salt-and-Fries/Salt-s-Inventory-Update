package com.salts_inventory_update.platform.fabric.api.client.networking.v1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.salts_inventory_update.platform.fabric.api.networking.v1.ForgeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ClientPlayNetworking {
    private static final Map<ResourceLocation, PlayChannelHandler> RECEIVERS = new ConcurrentHashMap<>();

    private ClientPlayNetworking() {
    }

    public static void registerGlobalReceiver(ResourceLocation id, PlayChannelHandler handler) {
        RECEIVERS.put(id, handler);
        ForgeNetworking.registerClientbound(id);
    }

    public static void receive(ForgeNetworking.NetworkMessage message) {
        PlayChannelHandler handler = RECEIVERS.get(message.id());
        if (handler != null) {
            Minecraft client = Minecraft.getInstance();
            FriendlyByteBuf buf = message.buffer();
            try {
                handler.receive(client, client.getConnection(), buf, PacketSender.INSTANCE);
            } finally {
                buf.release();
            }
        }
    }

    public static boolean canSend(ResourceLocation id) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null
            && ForgeNetworking.supportsServerbound(id)
            && ForgeNetworking.CHANNEL.isRemotePresent(listener.getConnection());
    }

    public static void send(ResourceLocation id, FriendlyByteBuf buf) {
        if (!canSend(id)) {
            buf.release();
            throw new IllegalStateException("Cannot send payload without a negotiated channel: " + id);
        }
        ForgeNetworking.CHANNEL.sendToServer(ForgeNetworking.message(id, buf));
    }

    public static boolean disconnectProtocolError(Component message) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener != null && listener.getConnection().isConnected()) {
            listener.getConnection().disconnect(message);
            return true;
        }
        return false;
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
