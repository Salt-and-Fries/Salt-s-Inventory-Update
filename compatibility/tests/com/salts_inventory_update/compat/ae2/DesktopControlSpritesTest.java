package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.compat.ae2.client.DesktopControlSprites;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DesktopControlSpritesTest {
    private static DesktopControlState state(DesktopWindowControl kind, boolean selected,
            boolean hovered, boolean pressed, boolean enabled) {
        return new DesktopControlState(kind, new DesktopWindowBounds(10, 20, 11, 11),
                hovered, pressed, enabled, selected, false);
    }

    @Test void everyButtonStaysInsideTheSharedTwelveColumnAtlas() {
        for (var kind : DesktopWindowControl.values()) {
            if (kind == DesktopWindowControl.RESIZE_GRIP) continue;
            for (int flags = 0; flags < 32; flags++) {
                var sprite = DesktopControlSprites.sprite(state(kind, (flags & 1) != 0,
                        (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0), (flags & 16) != 0);
                assertTrue(sprite.sourceX() >= 0 && sprite.sourceX() + 11 <= 132, kind.toString());
                assertTrue(sprite.sourceY() >= 0 && sprite.sourceY() + 11 <= 33, kind.toString());
            }
        }
    }

    @Test void pinAndLockUseStateColumnsWithoutLatchingThePressedRow() {
        assertEquals(new DesktopControlSprites.Sprite(7, 0),
                DesktopControlSprites.sprite(state(DesktopWindowControl.PIN, true, false, false, true), false));
        assertEquals(new DesktopControlSprites.Sprite(5, 1),
                DesktopControlSprites.sprite(state(DesktopWindowControl.LOCK, true, true, false, true), false));
        assertEquals(6, DesktopControlSprites.sprite(state(DesktopWindowControl.PIN, false, false, false, true), false).column());
        assertEquals(4, DesktopControlSprites.sprite(state(DesktopWindowControl.LOCK, false, false, false, true), false).column());
    }

    @Test void activeControlsAndPointerStatesMatchCoreFeedback() {
        for (var kind : new DesktopWindowControl[] { DesktopWindowControl.FOCUS,
                DesktopWindowControl.LINK, DesktopWindowControl.ELLIPSIS }) {
            assertEquals(2, DesktopControlSprites.sprite(state(kind, true, false, false, true), false).row());
        }
        assertEquals(1, DesktopControlSprites.sprite(state(DesktopWindowControl.CLOSE, false, true, false, true), false).row());
        assertEquals(2, DesktopControlSprites.sprite(state(DesktopWindowControl.CLOSE, false, true, true, true), false).row());
        assertEquals(0, DesktopControlSprites.sprite(state(DesktopWindowControl.FOCUS, true, true, true, false), false).row());
    }

    @Test void sortingAndOverflowUseTheirOwnSharedIcons() {
        assertEquals(1, DesktopControlSprites.sprite(state(DesktopWindowControl.SORT_INVENTORY, false, false, false, true), false).column());
        assertEquals(3, DesktopControlSprites.sprite(state(DesktopWindowControl.ELLIPSIS, false, false, false, true), false).column());
        assertEquals(10, DesktopControlSprites.sprite(state(DesktopWindowControl.SORT_WINDOWS, false, false, false, true), false).column());
        assertEquals(11, DesktopControlSprites.sprite(state(DesktopWindowControl.SORT_WINDOWS, false, false, false, true), true).column());
        assertThrows(IllegalArgumentException.class, () -> DesktopControlSprites.sprite(
                state(DesktopWindowControl.RESIZE_GRIP, false, false, false, true), false));
    }
}
