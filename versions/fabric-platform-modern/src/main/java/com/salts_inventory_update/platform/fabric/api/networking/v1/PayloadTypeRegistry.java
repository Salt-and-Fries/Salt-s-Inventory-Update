package com.salts_inventory_update.platform.fabric.api.networking.v1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PayloadTypeRegistry {
    private PayloadTypeRegistry() {
    }

    public static Registrar playC2S() {
        return new Registrar(registrar("playC2S", "serverboundPlay"));
    }

    public static Registrar playS2C() {
        return new Registrar(registrar("playS2C", "clientboundPlay"));
    }

    public static Registrar serverboundPlay() {
        return playC2S();
    }

    public static Registrar clientboundPlay() {
        return playS2C();
    }

    private static Object registrar(String preferredMethod, String fallbackMethod) {
        try {
            Class<?> registry = Class.forName("net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry");
            try {
                return registry.getMethod(preferredMethod).invoke(null);
            } catch (NoSuchMethodException ignored) {
                return registry.getMethod(fallbackMethod).invoke(null);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Fabric payload registry", exception);
        }
    }

    public static final class Registrar {
        private final Object delegate;

        private Registrar(Object delegate) {
            this.delegate = delegate;
        }

        public <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
        ) {
            try {
                Method method = delegate.getClass().getMethod("register", CustomPacketPayload.Type.class, StreamCodec.class);
                method.invoke(delegate, type, codec);
            } catch (NoSuchMethodException | IllegalAccessException exception) {
                throw new IllegalStateException("Unable to register Fabric payload type " + type.id(), exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException("Unable to register Fabric payload type " + type.id(), cause);
            }
        }
    }
}
