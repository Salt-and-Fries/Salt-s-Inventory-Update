package com.salts_inventory_update.layout;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanionPlacementTest {
    @Test void triesAnUnobscuredSideBeforeClampingOverTheOwner() {
        assertEquals(new DesktopWindowBounds(284, 40, 176, 52), CompanionPlacement.beside(new DesktopWindowBounds(100, 40, 180, 64), 176, 52, 640, 480));
        assertEquals(new DesktopWindowBounds(238, 40, 176, 52), CompanionPlacement.beside(new DesktopWindowBounds(418, 40, 176, 64), 176, 52, 640, 480));
        assertEquals(new DesktopWindowBounds(70, 108, 176, 52), CompanionPlacement.beside(new DesktopWindowBounds(70, 40, 176, 64), 176, 52, 320, 240));
    }
    @Test void retainsNativeSizeWhenNoAdjacentPlacementFits() {
        var placed = CompanionPlacement.beside(new DesktopWindowBounds(0, 0, 180, 64), 176, 52, 160, 48);
        assertEquals(176, placed.width()); assertEquals(52, placed.height()); assertEquals(0, placed.x()); assertEquals(0, placed.y());
        var boundary = CompanionPlacement.beside(new DesktopWindowBounds(Integer.MAX_VALUE - 100, 0, 100, 64), 176, 52, 640, 480);
        assertEquals(464, boundary.x(), "Offscreen owner coordinates cannot overflow adjacency calculations");
    }
}
