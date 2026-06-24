package com.salts_inventory_update.platform;

import com.salts_inventory_update.client.WindowedInventoryClient;

import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientPlatform {
    private static boolean initialized;

    private NeoForgeClientPlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        modBus.addListener(KeyBindingHelper::onRegisterKeyMappings);
        modBus.addListener(KeyMappingHelper::onRegisterKeyMappings);
        modBus.addListener(NeoForgeClientPlatform::onClientSetup);
        NeoForge.EVENT_BUS.addListener(ClientTickEvents::onStartClientTick);
        NeoForge.EVENT_BUS.addListener(ClientTickEvents::onEndClientTick);
        NeoForge.EVENT_BUS.addListener(ClientCommandRegistrationCallback.EVENT::onRegisterClientCommands);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        WindowedInventoryClient.initialize();
        ClientLifecycleEvents.fireStarted(Minecraft.getInstance());
        });
    }
}
