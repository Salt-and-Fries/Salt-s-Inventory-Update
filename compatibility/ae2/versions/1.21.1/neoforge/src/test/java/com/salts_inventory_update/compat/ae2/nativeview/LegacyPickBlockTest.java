package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;

class LegacyPickBlockTest {
    private abstract static class Listener implements GuiEventListener {
        @Override public void setFocused(boolean focused) { }
        @Override public boolean isFocused() { return false; }
    }

    @Test void scopedPickPreservesSlotOwnerAndNativeCoordinates() {
        var selected = new Slot(null, 3, 12, 24);
        var unrelated = new Listener() { };
        var owner = new Listener() {
            @Override public boolean mouseClicked(double x, double y, int button) {
                assertEquals(28.5, x); assertEquals(44.25, y); assertEquals(2, button);
                assertSame(selected, LegacyPickScope.slot(this)); assertNull(LegacyPickScope.slot(unrelated));
                assertTrue(LegacyPickScope.matches(null, button)); return true;
            }
        };
        assertTrue(LegacyPickScope.invoke(owner, selected, 28.5, 44.25)); assertNull(LegacyPickScope.slot(owner));
    }

    @Test void recursivePickCannotReplaceTheOriginalScope() {
        var selected = new Slot(null, 3, 12, 24);
        var nested = new Listener() { @Override public boolean mouseClicked(double x, double y, int button) { fail("Nested handler ran"); return false; } };
        var owner = new Listener() {
            @Override public boolean mouseClicked(double x, double y, int button) {
                assertFalse(LegacyPickScope.invoke(nested, selected, 0, 0));
                assertSame(selected, LegacyPickScope.slot(this)); return true;
            }
        };
        assertTrue(LegacyPickScope.invoke(owner, selected, 0, 0)); assertNull(LegacyPickScope.slot(owner));
    }

    @Test void failedNativeHandlerAlwaysReleasesItsScope() {
        var selected = new Slot(null, 3, 12, 24);
        var owner = new Listener() { @Override public boolean mouseClicked(double x, double y, int button) { throw new IllegalArgumentException("native failure"); } };
        assertThrows(IllegalArgumentException.class, () -> LegacyPickScope.invoke(owner, selected, 0, 0));
        assertNull(LegacyPickScope.slot(owner));
        var next = new Listener() { @Override public boolean mouseClicked(double x, double y, int button) { return true; } };
        assertTrue(LegacyPickScope.invoke(next, selected, 0, 0)); assertNull(LegacyPickScope.slot(next));
    }
}
