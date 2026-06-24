package net.fabricmc.fabric.api.networking.v1;

import java.util.Optional;
import java.util.function.Supplier;

import com.salts_inventory_update.SaltsInventoryUpdate;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static boolean initialized;

    private ForgeNetworking() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.registerMessage(
            0,
            NetworkMessage.class,
            NetworkMessage::encode,
            NetworkMessage::decode,
            ForgeNetworking::handle,
            Optional.empty()
        );
    }

    public static NetworkMessage message(ResourceLocation id, FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new NetworkMessage(id, data);
    }

    private static void handle(NetworkMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayNetworking.receive(message, context);
            } else {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.receive(message);
            }
        });
        context.setPacketHandled(true);
    }

    public static final class NetworkMessage {
        private final ResourceLocation id;
        private final byte[] data;

        private NetworkMessage(ResourceLocation id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public ResourceLocation id() {
            return id;
        }

        public FriendlyByteBuf buffer() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }

        private static void encode(NetworkMessage message, FriendlyByteBuf buf) {
            buf.writeResourceLocation(message.id);
            buf.writeByteArray(message.data);
        }

        private static NetworkMessage decode(FriendlyByteBuf buf) {
            return new NetworkMessage(buf.readResourceLocation(), buf.readByteArray());
        }
    }
}
