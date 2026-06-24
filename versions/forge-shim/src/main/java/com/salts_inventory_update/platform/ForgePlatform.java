package com.salts_inventory_update.platform;

import java.lang.reflect.InvocationTargetException;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ForgeNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.salts_inventory_update.debug.DesktopDebug;

public final class ForgePlatform {
    private static boolean initialized;

    private ForgePlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            DesktopDebug.log("forge platform server hooks already initialized");
            return;
        }
        initialized = true;

        DesktopDebug.log("forge platform server hooks initializing dist={}", FMLEnvironment.dist);
        ForgeNetworking.initialize();
        MinecraftForge.EVENT_BUS.addListener(ServerTickEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ServerPlayConnectionEvents::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(ServerLifecycleEvents::onServerStarting);
        DesktopDebug.log("forge platform server hooks registered");
    }

    public static void initializeClient(IEventBus modBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            DesktopDebug.log("forge client hooks skipped dist={}", FMLEnvironment.dist);
            return;
        }

        try {
            DesktopDebug.log("forge client hooks loading reflection target");
            Class<?> platform = Class.forName("com.salts_inventory_update.platform.ForgeClientPlatform");
            platform.getMethod("initialize", IEventBus.class).invoke(null, modBus);
            DesktopDebug.log("forge client hooks initialized");
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception;
            if (exception instanceof InvocationTargetException && ((InvocationTargetException) exception).getCause() != null) {
                cause = ((InvocationTargetException) exception).getCause();
            }
            throw new IllegalStateException("Failed to initialize Forge client hooks", cause);
        }
    }
}
