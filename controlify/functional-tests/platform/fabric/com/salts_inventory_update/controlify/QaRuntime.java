package com.salts_inventory_update.controlify.qa;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Loader services for the separate opt-in QA mod. */
public final class QaRuntime {
    private QaRuntime() { }
    public static void registerTick(Consumer<Minecraft> callback) {
        ClientTickEvents.END_CLIENT_TICK.register(callback::accept);
    }
    public static Map<String, String> loadedMods() {
        Map<String, String> mods = new TreeMap<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> mods.put(
                mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString()));
        return mods;
    }
}
