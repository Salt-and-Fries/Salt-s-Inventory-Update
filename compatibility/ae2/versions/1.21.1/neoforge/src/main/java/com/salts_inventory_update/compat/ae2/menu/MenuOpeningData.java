package com.salts_inventory_update.compat.ae2.menu;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/** AE's own fallback opening data when a normal native open has no loader-specific extra buffer. */
public final class MenuOpeningData {
    private record Entry(ResourceLocation type, byte[] data) {
    }

    private static final Map<Integer, Entry> PENDING = new LinkedHashMap<>();
    private static Object connection;
    private static boolean nativeDesktopOpening;

    private MenuOpeningData() {
    }

    public static void enableNativeDesktopOpening() {
        nativeDesktopOpening = true;
    }

    public static boolean nativeDesktopOpening() {
        return nativeDesktopOpening;
    }

    public static void receive(Object owner, int id, ResourceLocation type, byte[] data) {
        if (connection != owner) {
            PENDING.clear();
            connection = owner;
        }
        if (data.length > 32768) {
            throw new IllegalArgumentException("AE opening data exceeds its transport bound");
        }
        if (PENDING.size() >= 64) {
            PENDING.remove(PENDING.keySet().iterator().next());
        }
        PENDING.put(id, new Entry(type, data.clone()));
    }

    public static byte[] take(int id, ResourceLocation type) {
        var entry = PENDING.remove(id);
        if (entry == null || !entry.type().equals(type)) {
            throw new IllegalStateException("Missing AE opening data for " + type + " / " + id);
        }
        return entry.data();
    }
}
