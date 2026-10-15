package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTooltipCoordinatesTest {
    @Test void enlargedWindowQueuesTooltipAtItsDisplayedSlot() {
        try (var scope = WorkspaceTooltipCoordinates.enter(300, 40, 80, 120, 1.8)) {
            assertEquals(350, WorkspaceTooltipCoordinates.screenX(108));
            assertEquals(90, WorkspaceTooltipCoordinates.screenY(148));
        }
    }

    @Test void recipeGuideLeftOfNativeWindowUsesTheCombinedPresentationOrigin() {
        try (var scope = WorkspaceTooltipCoordinates.enter(12, 24, -70, 90, 0.5)) {
            assertEquals(37, WorkspaceTooltipCoordinates.screenX(-20));
            assertEquals(44, WorkspaceTooltipCoordinates.screenY(130));
        }
    }

    @Test void nestedWindowScopesAndExceptionsDoNotAffectOrdinaryTooltips() {
        try (var outer = WorkspaceTooltipCoordinates.enter(10, 20, 0, 0, 2)) {
            assertThrows(IllegalStateException.class, () -> {
                try (var inner = WorkspaceTooltipCoordinates.enter(100, 200, 50, 50, 0.5)) {
                    assertEquals(105, WorkspaceTooltipCoordinates.screenX(60));
                    throw new IllegalStateException();
                }
            });
            assertEquals(130, WorkspaceTooltipCoordinates.screenX(60));
        }
        assertEquals(60, WorkspaceTooltipCoordinates.screenX(60));
        assertEquals(80, WorkspaceTooltipCoordinates.screenY(80));
    }
}
