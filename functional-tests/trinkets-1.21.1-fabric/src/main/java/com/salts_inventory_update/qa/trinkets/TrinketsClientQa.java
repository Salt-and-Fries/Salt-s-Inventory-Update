package com.salts_inventory_update.qa.trinkets;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.network.DesktopPackets;
import dev.emi.trinkets.TrinketsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Exercises the actual authenticated desktop transfer transport; never writes client inventory. */
public final class TrinketsClientQa implements ClientModInitializer {
    private int ticks;
    @Override public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
            var command = ClientCommandManager.literal("trinketsqaclient");
            for (var mode : Map.of("quick", DesktopPackets.QUICK_TARGET_DEFAULT,
                    "equipment", DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT,
                    "storage", DesktopPackets.QUICK_TARGET_PLAYER_STORAGE).entrySet()) {
                command.then(ClientCommandManager.literal(mode.getKey()).then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(0, 4096))
                    .executes(c -> {
                        boolean sent = DesktopContainerClient.quickMoveSlot(0, IntegerArgumentType.getInteger(c, "slot"), mode.getValue(), 0);
                        if (!sent) throw new AssertionError("Desktop transport was not ready");
                        return 1;
                    })));
            }
            dispatcher.register(command);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 20 != 0 || mc.player == null) return;
            List<Map<String, Object>> slots = new ArrayList<>();
            for (int i = 0; i < mc.player.inventoryMenu.slots.size(); i++) {
                var slot = mc.player.inventoryMenu.slots.get(i);
                slots.add(Map.of("id", i, "key", DesktopPlayerMenuApi.slotKey(slot), "item", slot.getItem().toString()));
            }
            try {
                var output = Path.of(System.getProperty("salts.trinkets.qa")); Files.createDirectories(output);
                Files.writeString(output.resolve("client-state.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                    Map.of("slots", slots, "cursor", mc.player.inventoryMenu.getCarried().toString(),
                        "group", TrinketsClient.activeGroup == null ? "" : TrinketsClient.activeGroup.getName(),
                        "type", TrinketsClient.activeType == null ? "" : TrinketsClient.activeType.getName())));
            } catch (Exception failure) { throw new IllegalStateException(failure); }
        });
    }
}
