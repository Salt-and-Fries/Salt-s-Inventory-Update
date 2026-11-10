package com.salts_inventory_update.compat.aether.neoforge;
import com.salts_inventory_update.compat.aether.AetherEquipment;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class AetherNeoForgeTransport {
    private AetherNeoForgeTransport() { }
    public static void initialize(){com.salts_inventory_update.compat.platform.LoaderMenus.bus().addListener(AetherNeoForgeTransport::register);}
    private static void register(RegisterPayloadHandlersEvent event){event.registrar("1").optional().playToServer(Open.TYPE,Open.CODEC,(packet,context)->
        context.enqueueWork(()->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)AetherEquipment.receive(player,packet.owner());}));}
    public static void open(UUID owner){net.neoforged.neoforge.network.PacketDistributor.sendToServer(new Open(owner));}
    public record Open(UUID owner) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","aether/open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=new StreamCodec<>(){
            public Open decode(RegistryFriendlyByteBuf buffer){return new Open(buffer.readUUID());}
            public void encode(RegistryFriendlyByteBuf buffer,Open packet){buffer.writeUUID(packet.owner());}
        };
        @Override public Type<Open> type(){return TYPE;}
    }
}
