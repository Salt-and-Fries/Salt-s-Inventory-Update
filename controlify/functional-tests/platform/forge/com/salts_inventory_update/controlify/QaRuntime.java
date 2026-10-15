package com.salts_inventory_update.controlify.qa;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class QaRuntime {
    private QaRuntime() { }
    public static void registerTick(Consumer<Minecraft> callback) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) callback.accept(Minecraft.getInstance());
        });
    }
    public static Map<String, String> loadedMods() {
        Map<String, String> mods = new TreeMap<>();
        ModList.get().getMods().forEach(mod -> mods.put(mod.getModId(), mod.getVersion().toString()));
        return mods;
    }
}
