package com.salts_inventory_update.compat.accessories.neoforge;
import com.salts_inventory_update.compat.accessories.AccessoriesTransport;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class AccessoriesNeoForgeTransport {
    private AccessoriesNeoForgeTransport() { }
    public static void initialize(){com.salts_inventory_update.compat.platform.LoaderMenus.bus().addListener(AccessoriesNeoForgeTransport::register);}
    private static void register(RegisterPayloadHandlersEvent event){event.registrar("1").optional().playToServer(Open.TYPE,Open.CODEC,(packet,context)->
        context.enqueueWork(()->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)AccessoriesTransport.receive(player,packet.owner());}));}
    public static void open(java.util.UUID owner){net.neoforged.neoforge.network.PacketDistributor.sendToServer(new Open(owner));}

    public record Open(java.util.UUID owner) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(com.salts_inventory_update.compat.accessories.AccessoriesIntegration.id("accessories/open"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Open> CODEC=new net.minecraft.network.codec.StreamCodec<>(){
            public Open decode(net.minecraft.network.RegistryFriendlyByteBuf buffer){return new Open(buffer.readUUID());}
            public void encode(net.minecraft.network.RegistryFriendlyByteBuf buffer,Open packet){buffer.writeUUID(packet.owner());}
        };
        @Override public Type<Open> type(){return TYPE;}
    }
}
