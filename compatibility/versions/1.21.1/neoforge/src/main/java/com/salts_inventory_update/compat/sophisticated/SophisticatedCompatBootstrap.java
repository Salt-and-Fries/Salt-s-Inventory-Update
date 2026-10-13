package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.salts_inventory_update.compat.CompatibilityLog;

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
            CompatibilityLog.log("Sophisticated desktop integration inactive: no supported mods installed");
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
        CompatibilityLog.log("Sophisticated desktop integration initialized client={} backpacks={} storage={}", client, backpacks, storage);
    }

    private static void invoke(String className, String methodName) {
        String modId = className.contains("Backpacks") ? "sophisticatedbackpacks"
            : className.contains("Storage") ? "sophisticatedstorage" : "sophisticatedcore";
        com.salts_inventory_update.compat.CompatibilityBootstrap.initialize(modId, className, methodName);
    }
}
