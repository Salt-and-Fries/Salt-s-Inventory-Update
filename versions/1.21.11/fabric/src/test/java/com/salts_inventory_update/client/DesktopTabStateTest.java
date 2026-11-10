package com.salts_inventory_update.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopTabStateTest {
    @Test void disabledDropdownStaysHiddenOnDesktopButKeepsBothEscapeRoutes() {
        assertFalse(DesktopTabState.shouldShow(false, false, false));
        assertTrue(DesktopTabState.shouldShow(false, true, false));
        assertTrue(DesktopTabState.shouldShow(false, false, true));
        assertTrue(DesktopTabState.shouldShow(false, true, true));
    }

    @Test void enabledDropdownRemainsAvailableWithoutHoldingInventory() {
        assertTrue(DesktopTabState.shouldShow(true, false, false));
        assertTrue(DesktopTabState.shouldShow(true, false, true));
    }

    @Test void tuckedTabIsDiscoverableOnlyAtTheTopCenter() {
        var tab = new DesktopTabState();
        assertEquals(-29, tab.y());
        assertTrue(tab.contains(800, 400, 0));
        assertFalse(tab.contains(800, 400, 3));
        assertFalse(tab.contains(800, 350, 0));
        assertFalse(tab.contains(800, 400, -1));
    }

    @Test void hoverCanFollowTheTabDownAndLeavingRetractsIt() {
        var tab = new DesktopTabState();
        tab.update(1000, 800, 400, 0, false);
        for (int i = 1; i <= 4; i++) tab.update(1000 + i * 40, 800, 400, 0, false);
        assertEquals(0, tab.y());
        tab.update(1200, 800, 400, 25, false);
        assertEquals(0, tab.y());
        for (int i = 1; i <= 4; i++) tab.update(1200 + i * 40, 800, 100, 100, false);
        assertEquals(-29, tab.y());
    }

    @Test void inventoryHoldOpensWithoutHoverAndResizeKeepsItCentered() {
        var tab = new DesktopTabState();
        tab.update(1000, 800, 0, 500, true);
        for (int i = 1; i <= 4; i++) tab.update(1000 + i * 40, 800, 0, 500, true);
        assertEquals(0, tab.y());
        assertEquals(352, tab.x(800));
        assertEquals(112, tab.x(320));
        assertFalse(tab.contains(320, 352, 10));
        assertTrue(tab.contains(320, 160, 10));
    }

    @Test void staleClockAndLongFrameCannotTeleportTabOrLeaveAStuckAction() {
        var tab = new DesktopTabState();
        tab.update(1000, 800, 0, 100, true);
        tab.update(900, 800, 0, 100, true);
        assertEquals(-29, tab.y());
        tab.update(9000, 800, 0, 100, true);
        assertTrue(tab.y() < 0);
        tab.reset();
        assertEquals(-29, tab.y());
    }
}
