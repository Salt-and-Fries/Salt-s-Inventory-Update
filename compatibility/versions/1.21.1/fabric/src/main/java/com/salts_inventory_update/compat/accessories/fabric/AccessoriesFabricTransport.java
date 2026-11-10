package com.salts_inventory_update.compat.accessories.fabric;
import com.salts_inventory_update.compat.accessories.AccessoriesTransport;
public final class AccessoriesFabricTransport {
    private AccessoriesFabricTransport() { }
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(Open.TYPE,Open.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Open.TYPE,(packet,context)->
            context.server().execute(()->AccessoriesTransport.receive(context.player(),packet.owner())));
    }
    public static void open(java.util.UUID owner){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new Open(owner));}

    public record Open(java.util.UUID owner) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(com.salts_inventory_update.compat.accessories.AccessoriesIntegration.id("accessories/open"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Open> CODEC=new net.minecraft.network.codec.StreamCodec<>(){
            public Open decode(net.minecraft.network.RegistryFriendlyByteBuf buffer){return new Open(buffer.readUUID());}
            public void encode(net.minecraft.network.RegistryFriendlyByteBuf buffer,Open packet){buffer.writeUUID(packet.owner());}
        };
        @Override public Type<Open> type(){return TYPE;}
    }
}
