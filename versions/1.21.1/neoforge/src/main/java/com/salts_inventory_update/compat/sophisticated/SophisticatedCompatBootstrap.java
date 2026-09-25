package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.platform.fabric.api.networking.v1.PayloadTypeRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** NeoForge 26.2-only guarded bootstrap for the optional Sophisticated integrations. */
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

        PayloadTypeRegistry.clientboundPlay().register(DesktopMenuOpenDataPayload.TYPE, DesktopMenuOpenDataPayload.CODEC);
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
