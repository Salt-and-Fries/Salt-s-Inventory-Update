package com.salts_inventory_update.platform.fabric.api.networking.v1;

import java.util.Set;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.protocol.DesktopProtocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeNetworking {
    // Keep the Forge transport version stable so the application-level hello can
    // diagnose legacy desktop protocol peers instead of FML rejecting them first.
    private static final String TRANSPORT_VERSION = "1";
    private static final int MAX_ENVELOPE_BYTES = DesktopProtocol.MAX_ENVELOPE_BYTES;
    private static final int MAX_ID_LENGTH = 128;
    private static final Set<ResourceLocation> SERVERBOUND_IDS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> CLIENTBOUND_IDS = ConcurrentHashMap.newKeySet();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "main"),
        () -> TRANSPORT_VERSION,
        NetworkRegistry.acceptMissingOr(TRANSPORT_VERSION),
        NetworkRegistry.acceptMissingOr(TRANSPORT_VERSION)
    );

    private static boolean initialized;

    private ForgeNetworking() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerBuiltInDirections();
        CHANNEL.registerMessage(
            0,
            NetworkMessage.class,
            NetworkMessage::encode,
            NetworkMessage::decode,
            ForgeNetworking::handle,
            Optional.empty()
        );
    }

    private static void registerBuiltInDirections() {
        for (String path : new String[] {
            "desktop_hello", "desktop_mode", "desktop_authenticated"
        }) {
            registerServerbound(DesktopPackets.id(path));
        }
        for (String path : new String[] {
            "desktop_hello_ack", "desktop_session_authorization", "inventory_expansion_sync",
            "desktop_open_session", "desktop_slot", "desktop_data",
            "desktop_carried", "desktop_ghost_recipe", "desktop_session_closed",
            "desktop_merchant_offers", "desktop_custom", "desktop_session_visibility"
        }) {
            registerClientbound(DesktopPackets.id(path));
        }
    }

    public static NetworkMessage message(ResourceLocation id, FriendlyByteBuf buf) {
        try {
            if (!SERVERBOUND_IDS.contains(id) && !CLIENTBOUND_IDS.contains(id)) {
                throw new IllegalArgumentException("Unknown Salt's Inventory Update payload: " + id);
            }
            if (buf.readableBytes() > MAX_ENVELOPE_BYTES) {
                throw new IllegalArgumentException("Salt's Inventory Update payload is too large: " + buf.readableBytes());
            }
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new NetworkMessage(id, data);
        } finally {
            buf.release();
        }
    }

    private static void handle(NetworkMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                if (message.rejectionReason() != null) {
                    rejectProtocol(context, null, message.rejectionReason());
                } else if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER && SERVERBOUND_IDS.contains(message.id())) {
                    ServerPlayNetworking.receive(message, context);
                } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT && CLIENTBOUND_IDS.contains(message.id())) {
                    com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking.receive(message);
                } else {
                    rejectProtocol(context, message.id(), "unknown or wrong-direction payload");
                }
            } catch (RuntimeException exception) {
                rejectProtocol(context, message.id(), "handler failure: " + exception.getClass().getSimpleName());
            }
        });
        context.setPacketHandled(true);
    }

    private static void rejectProtocol(NetworkEvent.Context context, ResourceLocation id, String reason) {
        Component message = Component.literal("Salt's Inventory Update network protocol error.");
        boolean closed = false;
        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerPlayer sender = context.getSender();
            if (sender != null && sender.connection != null && sender.connection.connection.isConnected()) {
                sender.connection.disconnect(message);
                closed = true;
            }
        } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            closed = com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking.disconnectProtocolError(message);
        }
        if (closed) {
            SaltsInventoryUpdate.LOGGER.warn(
                "Closing Forge desktop protocol connection direction={} payload={} reason={}",
                context.getDirection(),
                id == null ? "<malformed>" : id,
                reason
            );
        }
    }

    public static void registerServerbound(ResourceLocation id) {
        SERVERBOUND_IDS.add(id);
    }

    public static void registerClientbound(ResourceLocation id) {
        CLIENTBOUND_IDS.add(id);
    }

    public static boolean supportsServerbound(ResourceLocation id) {
        return SERVERBOUND_IDS.contains(id);
    }

    public static boolean supportsClientbound(ResourceLocation id) {
        return CLIENTBOUND_IDS.contains(id);
    }

    public static final class NetworkMessage {
        private final ResourceLocation id;
        private final byte[] data;
        private final String rejectionReason;

        private NetworkMessage(ResourceLocation id, byte[] data) {
            this.id = id;
            if (data.length > MAX_ENVELOPE_BYTES) {
                throw new IllegalArgumentException("Salt's Inventory Update payload is too large: " + data.length);
            }
            this.data = data.clone();
            this.rejectionReason = null;
        }

        private NetworkMessage(String rejectionReason) {
            this.id = null;
            this.data = new byte[0];
            this.rejectionReason = rejectionReason;
        }

        public ResourceLocation id() {
            return id;
        }

        public FriendlyByteBuf buffer() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }

        private String rejectionReason() {
            return rejectionReason;
        }

        private static void encode(NetworkMessage message, FriendlyByteBuf buf) {
            if (message.rejectionReason != null) {
                throw new IllegalArgumentException("Cannot encode a rejected Forge network envelope");
            }
            buf.writeUtf(message.id.toString(), MAX_ID_LENGTH);
            buf.writeByteArray(message.data);
        }

        private static NetworkMessage decode(FriendlyByteBuf buf) {
            try {
                ResourceLocation id = new ResourceLocation(buf.readUtf(MAX_ID_LENGTH));
                byte[] data = buf.readByteArray(MAX_ENVELOPE_BYTES);
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing bytes in Forge network envelope");
                }
                return new NetworkMessage(id, data);
            } catch (RuntimeException exception) {
                return new NetworkMessage("malformed envelope: " + exception.getClass().getSimpleName());
            }
        }
    }
}
