package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class LegacyPreferencesTest {
    @Test void missingOrWrongTypeUsesTheForkDefault() {
        var preferences = new CompoundTag();
        assertEquals("closed", LegacyPreferences.stringOr(preferences, "panel", "closed"));
        preferences.putInt("panel", 12);
        assertEquals("closed", LegacyPreferences.stringOr(preferences, "panel", "closed"));
    }

    @Test void presentEmptyStringRemainsAnExplicitPreference() {
        var preferences = new CompoundTag(); preferences.putString("panel", "");
        assertEquals("", LegacyPreferences.stringOr(preferences, "panel", "closed"));
        preferences.putString("panel", "upgrades");
        assertEquals("upgrades", LegacyPreferences.stringOr(preferences, "panel", "closed"));
    }
}
