package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.salts_inventory_update.debug.DesktopDebug;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Forge 1.20.1 guarded bootstrap for the optional Sophisticated integrations. */
public final class SophisticatedCompatBootstrap {
    private static boolean initialized;

    private SophisticatedCompatBootstrap() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        boolean backpacks = ModList.get().isLoaded("sophisticatedbackpacks");
        boolean storage = ModList.get().isLoaded("sophisticatedstorage");
        if (!backpacks && !storage) {
            DesktopDebug.log("Sophisticated desktop integration inactive: no supported mods installed");
            return;
        }

        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> initializeSide(false, backpacks, storage)));
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> initializeSide(true, backpacks, storage)));
    }

    private static void initializeSide(boolean client, boolean backpacks, boolean storage) {
        invoke("com.salts_inventory_update.compat.sophisticated.SophisticatedCoreCompat", client ? "initializeClient" : "initializeCommon");
        if (backpacks) {
            invoke("com.salts_inventory_update.compat.sophisticated.SophisticatedBackpacksCompat", client ? "initializeClient" : "initializeCommon");
        }
        if (storage) {
            invoke("com.salts_inventory_update.compat.sophisticated.SophisticatedStorageCompat", client ? "initializeClient" : "initializeCommon");
        }
        DesktopDebug.log("Sophisticated desktop integration initialized client={} backpacks={} storage={}", client, backpacks, storage);
    }

    private static void invoke(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
            throw new IllegalStateException("Failed to initialize optional Sophisticated integration " + className, cause);
        }
    }
}
