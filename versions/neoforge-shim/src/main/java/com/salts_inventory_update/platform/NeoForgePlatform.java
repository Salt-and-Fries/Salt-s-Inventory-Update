package com.salts_inventory_update.platform;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgePlatform {
    private static boolean initialized;

    private NeoForgePlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        modBus.addListener(PayloadTypeRegistry::register);
        NeoForge.EVENT_BUS.addListener(ServerTickEvents::onEndServerTick);
        NeoForge.EVENT_BUS.addListener(ServerPlayConnectionEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ServerLifecycleEvents::onServerStarting);
    }

    public static void initializeClient(IEventBus modBus) {
        if (!isClient()) {
            return;
        }

        try {
            Class<?> platform = Class.forName("com.salts_inventory_update.platform.NeoForgeClientPlatform");
            platform.getMethod("initialize", IEventBus.class).invoke(null, modBus);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception;
            if (exception instanceof InvocationTargetException && ((InvocationTargetException) exception).getCause() != null) {
                cause = ((InvocationTargetException) exception).getCause();
            }
            throw new IllegalStateException("Failed to initialize NeoForge client hooks", cause);
        }
    }

    private static boolean isClient() {
        Object dist = readStaticMethod("net.neoforged.fml.loading.FMLEnvironment", "getDist");
        if (dist == null) {
            dist = readStaticField("net.neoforged.fml.loading.FMLEnvironment", "dist");
        }
        if (dist == null) {
            dist = readStaticMethod("net.neoforged.fml.loading.FMLLoader", "getDist");
        }
        if (dist == null) {
            Object loader = readStaticMethod("net.neoforged.fml.loading.FMLLoader", "getCurrent");
            dist = readInstanceMethod(loader, "getDist");
        }
        return dist != null && "CLIENT".equals(dist.toString());
    }

    private static Object readStaticMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            if (Modifier.isStatic(method.getModifiers())) {
                return method.invoke(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object readStaticField(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getField(fieldName);
            if (Modifier.isStatic(field.getModifiers())) {
                return field.get(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object readInstanceMethod(Object receiver, String methodName) {
        if (receiver == null) {
            return null;
        }
        try {
            Method method = receiver.getClass().getMethod(methodName);
            return method.invoke(receiver);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
