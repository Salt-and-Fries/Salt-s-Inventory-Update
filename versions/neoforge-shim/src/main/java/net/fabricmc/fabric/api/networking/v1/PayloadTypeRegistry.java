package net.fabricmc.fabric.api.networking.v1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.salts_inventory_update.SaltsInventoryUpdate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PayloadTypeRegistry {
    private static final List<Entry<?>> SERVERBOUND = new ArrayList<>();
    private static final List<Entry<?>> CLIENTBOUND = new ArrayList<>();

    private PayloadTypeRegistry() {
    }

    public static Registrar playC2S() {
        return new Registrar(SERVERBOUND);
    }

    public static Registrar playS2C() {
        return new Registrar(CLIENTBOUND);
    }

    public static Registrar serverboundPlay() {
        return playC2S();
    }

    public static Registrar clientboundPlay() {
        return playS2C();
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SaltsInventoryUpdate.MOD_ID).optional();
        Map<CustomPacketPayload.Type<?>, Entry<?>> serverbound = entriesByType(SERVERBOUND);
        Map<CustomPacketPayload.Type<?>, Entry<?>> clientbound = entriesByType(CLIENTBOUND);

        for (Map.Entry<CustomPacketPayload.Type<?>, Entry<?>> entry : serverbound.entrySet()) {
            if (clientbound.containsKey(entry.getKey())) {
                registerBidirectional(registrar, entry.getValue());
            } else {
                registerServerbound(registrar, entry.getValue());
            }
        }
        for (Map.Entry<CustomPacketPayload.Type<?>, Entry<?>> entry : clientbound.entrySet()) {
            if (!serverbound.containsKey(entry.getKey())) {
                registerClientbound(registrar, entry.getValue());
            }
        }
    }

    private static Map<CustomPacketPayload.Type<?>, Entry<?>> entriesByType(List<Entry<?>> entries) {
        Map<CustomPacketPayload.Type<?>, Entry<?>> byType = new LinkedHashMap<>();
        for (Entry<?> entry : entries) {
            byType.putIfAbsent(entry.type(), entry);
        }
        return byType;
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, Entry<T> entry) {
        registrar.playToServer(entry.type(), entry.codec(), ServerPlayNetworking::receive);
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, Entry<T> entry) {
        registrar.playToClient(entry.type(), entry.codec(), net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking::receive);
    }

    private static <T extends CustomPacketPayload> void registerBidirectional(PayloadRegistrar registrar, Entry<T> entry) {
        IPayloadHandler<T> serverHandler = ServerPlayNetworking::receive;
        IPayloadHandler<T> clientHandler = net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking::receive;
        if (registerBidirectionalWithClientHandler(registrar, entry, serverHandler, clientHandler)) {
            return;
        }
        registrar.playBidirectional(entry.type(), entry.codec(), PayloadTypeRegistry::receiveBidirectional);
    }

    private static <T extends CustomPacketPayload> boolean registerBidirectionalWithClientHandler(
        PayloadRegistrar registrar,
        Entry<T> entry,
        IPayloadHandler<T> serverHandler,
        IPayloadHandler<T> clientHandler
    ) {
        try {
            Method method = PayloadRegistrar.class.getMethod(
                "playBidirectional",
                CustomPacketPayload.Type.class,
                StreamCodec.class,
                IPayloadHandler.class,
                IPayloadHandler.class
            );
            method.invoke(registrar, entry.type(), entry.codec(), serverHandler, clientHandler);
            return true;
        } catch (NoSuchMethodException exception) {
            return false;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to register bidirectional payload " + entry.type().id(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Unable to register bidirectional payload " + entry.type().id(), cause);
        }
    }

    private static <T extends CustomPacketPayload> void receiveBidirectional(T payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            ServerPlayNetworking.receive(payload, context);
        } else {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.receive(payload, context);
        }
    }

    private record Entry<T extends CustomPacketPayload>(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
    }

    public static final class Registrar {
        private final List<Entry<?>> entries;

        private Registrar(List<Entry<?>> entries) {
            this.entries = entries;
        }

        public <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
        ) {
            for (Entry<?> entry : entries) {
                if (entry.type().equals(type)) {
                    return;
                }
            }
            entries.add(new Entry<>(type, codec));
        }
    }
}
