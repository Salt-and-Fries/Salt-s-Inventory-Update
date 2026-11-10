package com.salts_inventory_update.compat.aether.fabric;
import com.salts_inventory_update.compat.aether.AetherEquipment;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public final class AetherFabricTransport {
    private AetherFabricTransport() { }
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(Open.TYPE,Open.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Open.TYPE,(packet,context)->
            context.server().execute(()->AetherEquipment.receive(context.player(),packet.owner())));
    }
    public static void open(UUID owner){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new Open(owner));}
    public record Open(UUID owner) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","aether/open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=new StreamCodec<>(){
            public Open decode(RegistryFriendlyByteBuf buffer){return new Open(buffer.readUUID());}
            public void encode(RegistryFriendlyByteBuf buffer,Open packet){buffer.writeUUID(packet.owner());}
        };
        @Override public Type<Open> type(){return TYPE;}
    }
}
