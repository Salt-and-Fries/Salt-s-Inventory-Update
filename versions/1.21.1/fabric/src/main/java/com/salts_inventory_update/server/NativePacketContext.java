package com.salts_inventory_update.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Carries the real connection context into native codecs serialized off the wire. */
final class NativePacketContext {
    private static final Method SUPPLY=findSupply();
    private NativePacketContext() { }

    private static Method findSupply() {
        try {
            Class<?> context=Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContext");
            Class<?> provider=Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider");
            return context.getMethod("supplyWithContext",provider,Supplier.class);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException incompatible) {
            throw new IllegalStateException("Unsupported native packet context API",incompatible);
        }
    }

    static void run(ServerPlayer player,Runnable action) {
        supply(player,()->{action.run();return null;});
    }

    @SuppressWarnings("unchecked")
    static <T> T supply(ServerPlayer player,Supplier<T> action) {
        if(SUPPLY==null)return action.get();
        if(!SUPPLY.getParameterTypes()[0].isInstance(player.connection))
            throw new IllegalStateException("Native packet context is missing from the player connection");
        try {
            return (T)SUPPLY.invoke(null,player.connection,action);
        } catch (InvocationTargetException failure) {
            Throwable cause=failure.getCause();
            if(cause instanceof RuntimeException runtime)throw runtime;
            if(cause instanceof Error error)throw error;
            throw new IllegalStateException("Native packet serialization failed",cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native packet context invocation failed",failure);
        }
    }
}
