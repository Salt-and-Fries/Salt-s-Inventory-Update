package com.salts_inventory_update.client;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TopDropdownConfigTest {
    private final Gson gson = new Gson();

    @Test void existingConfigsKeepTheDropdownEnabled() {
        var config = gson.fromJson("{\"persistentWindows\":true}", SaltsInventoryConfig.ConfigFile.class);
        assertTrue(config.showTopDropdown);
        assertTrue(config.persistentWindows);
    }

    @Test void disabledPreferenceSurvivesJsonRoundTrip() {
        var config = new SaltsInventoryConfig.ConfigFile();
        config.showTopDropdown = false;
        config.eHoldCloseAllSeconds = 1.25D;
        var reloaded = gson.fromJson(gson.toJson(config), SaltsInventoryConfig.ConfigFile.class);
        assertFalse(reloaded.showTopDropdown);
        assertEquals(1.25D, reloaded.eHoldCloseAllSeconds);
    }

    @Test void resetDefaultsRestoresTheDropdown() {
        var config = new SaltsInventoryConfig.ConfigFile();
        config.showTopDropdown = false;
        config.resetToDefaults();
        assertTrue(config.showTopDropdown);
    }
}
