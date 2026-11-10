package com.salts_inventory_update.compat.toms_storage.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Supplies the real connection to optional native item-codec hooks off the wire. */
final class TerminalPacketContext {
    private static final Method SUPPLY = findSupply();
    private TerminalPacketContext() { }

    private static Method findSupply() {
        try {
            Class<?> context = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContext");
            Class<?> provider = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider");
            return context.getMethod("supplyWithContext", provider, Supplier.class);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Unsupported native packet context API", incompatible);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T supply(Object connection, Supplier<T> encoder) {
        if (SUPPLY == null) return encoder.get();
        if (!SUPPLY.getParameterTypes()[0].isInstance(connection))
            throw new IllegalStateException("Native packet context is missing from the client connection");
        try {
            return (T) SUPPLY.invoke(null, connection, encoder);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Native item serialization failed", cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native packet context invocation failed", failure);
        }
    }
}
