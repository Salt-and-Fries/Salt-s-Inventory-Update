package com.salts_inventory_update.compat.quark.neoforge;

import com.salts_inventory_update.compat.quark.QuarkWindows;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Opening requests address only the sender's equipped bag; mutations use owned SIU sessions. */
public final class QuarkNeoForgeTransport {
    private QuarkNeoForgeTransport() { }
    public static void initialize() { com.salts_inventory_update.compat.platform.LoaderMenus.bus().addListener(QuarkNeoForgeTransport::register); }
    private static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(Open.TYPE,Open.CODEC,(packet,context)->context.enqueueWork(()->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)QuarkWindows.openBackpack(player,packet.owner());
        }));
    }
    public static void open(UUID owner) { PacketDistributor.sendToServer(new Open(owner)); }
    public record Open(UUID owner) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(QuarkWindows.id("quark/backpack_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=new StreamCodec<>() {
            @Override public Open decode(RegistryFriendlyByteBuf b) { return new Open(b.readUUID()); }
            @Override public void encode(RegistryFriendlyByteBuf b,Open p) { b.writeUUID(p.owner()); }
        };
        @Override public Type<Open> type() { return TYPE; }
    }
}
