package com.salts_inventory_update.client;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class WindowFeatureRemovalTest {
    @Test void oldConfigurationsRetainOtherSettingsAndDiscardRemovedOptions() {
        Gson gson = new Gson();
        var config = gson.fromJson("""
            {"persistentWindows":true,"globalPins":true,"enableGhostPins":true,
             "minimizableWindows":true,"ghostWindowOpacity":0.8,"eHoldCloseAllSeconds":1.25}
            """, SaltsInventoryConfig.ConfigFile.class);
        assertTrue(config.persistentWindows);
        assertTrue(config.globalPins);
        assertEquals(1.25D, config.eHoldCloseAllSeconds);
        String saved = gson.toJson(config);
        for (String removed : List.of("enableGhostPins", "minimizableWindows", "ghostWindowOpacity")) {
            assertFalse(saved.contains(removed));
            assertThrows(NoSuchFieldException.class, () -> SaltsInventoryConfig.ConfigFile.class.getDeclaredField(removed));
        }
    }

    @Test void oldGhostLayoutsBecomeOrdinaryPinsAndPinningHasOnlyTwoStates() {
        var saved = new DesktopWindowStateStore.WindowState();
        saved.pinMode = "GHOST_PINNED";
        assertEquals(PinMode.PINNED, saved.pinMode());
        assertEquals(PinMode.PINNED, PinMode.UNPINNED.next());
        assertEquals(PinMode.UNPINNED, PinMode.PINNED.next());
        assertEquals(2, PinMode.values().length);
    }

    @Test void integrationsCannotCallRemovedWindowOrServerHooks() {
        for (Class<?> type : List.of(DesktopWindowContext.class, DesktopWindowDefinition.class,
                DesktopServerSessionContext.class, DesktopServerWindowHandler.class)) {
            assertFalse(Arrays.stream(type.getMethods()).anyMatch(method -> List.of(
                "minimized", "ghosted", "unghosted", "ghostPinned", "pinChanged").contains(method.getName())), type.getName());
        }
        for (String removed : List.of("minimized", "ghosted")) {
            assertThrows(NoSuchFieldException.class, () -> {
                Class<?> window = Arrays.stream(InventoryDesktopScreen.class.getDeclaredClasses())
                    .filter(type -> type.getSimpleName().equals("InventoryWindow")).findFirst().orElseThrow();
                window.getDeclaredField(removed);
            });
        }
    }
}
