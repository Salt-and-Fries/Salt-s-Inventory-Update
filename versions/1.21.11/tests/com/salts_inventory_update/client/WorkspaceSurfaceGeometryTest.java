package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WorkspaceSurfaceGeometryTest {
    @Test void normalInventoryKeepsHotbarImmediatelyBelowItsOccupiedRows() {
        assertEquals(108, WorkspaceSurfaceGeometry.compactInventoryHeight(27, 9, 8, 16, 8, 18, 22));
        assertEquals(126, WorkspaceSurfaceGeometry.compactInventoryHeight(28, 9, 8, 16, 8, 18, 22));
        assertEquals(198, WorkspaceSurfaceGeometry.compactInventoryHeight(100, 9, 8, 16, 8, 18, 22));
    }

    @Test void widerInventoriesAndEmptyViewsDoNotReserveBlankRows() {
        assertEquals(90, WorkspaceSurfaceGeometry.compactInventoryHeight(27, 14, 8, 16, 8, 18, 22));
        assertEquals(72, WorkspaceSurfaceGeometry.compactInventoryHeight(0, 9, 8, 16, 8, 18, 22));
    }

    @Test void attachedGuideExpandsTheFittedExtentWithoutStretchingTheInventoryFrame() {
        var frame = new DesktopWindowBounds(300, 100, 180, 110);
        var guide = new DesktopWindowBounds(119, 100, 177, 166);
        assertEquals(new DesktopWindowBounds(119, 100, 361, 166), WorkspaceSurfaceGeometry.union(frame, guide));
        assertEquals(frame, WorkspaceSurfaceGeometry.union(frame, new DesktopWindowBounds(310, 120, 10, 10)));
    }

    @Test void guideAboveOrLeftOfNativeOriginRemainsPartOfTheFittedSurface() {
        assertEquals(new DesktopWindowBounds(-181, -20, 361, 166), WorkspaceSurfaceGeometry.union(
            new DesktopWindowBounds(0, 0, 180, 110), new DesktopWindowBounds(-181, -20, 177, 166)));
    }
}
