package com.salts_inventory_update.compat.ae2.nativeview;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Preserves the fork's typed preference defaults on the older NBT API. */
public final class LegacyPreferences {
    private LegacyPreferences() { }
    public static String stringOr(CompoundTag preferences, String key, String fallback) {
        return preferences.contains(key, Tag.TAG_STRING) ? preferences.getString(key) : fallback;
    }
}
