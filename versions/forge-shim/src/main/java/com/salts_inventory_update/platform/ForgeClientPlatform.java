package com.salts_inventory_update.platform;

import com.salts_inventory_update.client.WindowedInventoryClient;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ForgeClientPlatform {
    private static boolean initialized;

    private ForgeClientPlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        modBus.addListener(KeyBindingHelper::onRegisterKeyMappings);
        modBus.addListener(ForgeClientPlatform::onClientSetup);
        modBus.addListener(ClientLifecycleEvents::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientTickEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientCommandRegistrationCallback.EVENT::onRegisterClientCommands);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(WindowedInventoryClient::initialize);
    }
}
