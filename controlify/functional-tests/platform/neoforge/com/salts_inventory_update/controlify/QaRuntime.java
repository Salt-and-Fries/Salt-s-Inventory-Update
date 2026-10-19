package com.salts_inventory_update.controlify.qa;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class QaRuntime {
    private QaRuntime() { }
    public static void registerTick(Consumer<Minecraft> callback) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> callback.accept(Minecraft.getInstance()));
    }
    public static Map<String, String> loadedMods() {
        Map<String, String> mods = new TreeMap<>();
        ModList.get().getMods().forEach(mod -> mods.put(mod.getModId(), mod.getVersion().toString()));
        return mods;
    }
}
