package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.input.CharacterEvent;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import java.util.List;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

class LegacyInputTest {
    private static final class NativeWidget implements GuiEventListener {
        List<?> received;
        boolean accepted;
        @Override public void setFocused(boolean focused) { }
        @Override public boolean isFocused() { return false; }
        @Override public boolean mouseClicked(double x, double y, int button) {
            received = List.of("click", x, y, button); return accepted;
        }
        @Override public boolean mouseReleased(double x, double y, int button) {
            received = List.of("release", x, y, button); return accepted;
        }
        @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
            received = List.of("drag", x, y, button, dx, dy); return accepted;
        }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            received = List.of("key", key, scan, modifiers); return accepted;
        }
        @Override public boolean keyReleased(int key, int scan, int modifiers) {
            received = List.of("key-release", key, scan, modifiers); return accepted;
        }
        @Override public boolean charTyped(char value, int modifiers) {
            received = List.of("character", value, modifiers); return accepted;
        }
    }

    @Test void rightClickNormalizationRetainsNativeCoordinatesAndHandlerResult() {
        var nativeWidget = new NativeWidget();
        var rightClick = new MouseButtonEvent(17.25, -31.75, 1);
        var widgetClick = LegacyInput.leftButton(rightClick);
        assertFalse(LegacyInput.mouseClicked(nativeWidget, widgetClick, true));
        assertEquals(List.of("click", 17.25, -31.75, 0), nativeWidget.received);
        assertEquals(1, rightClick.button());
        nativeWidget.accepted = true;
        assertTrue(LegacyInput.mouseClicked(nativeWidget, rightClick, false));
        assertEquals(List.of("click", 17.25, -31.75, 1), nativeWidget.received);
    }

    @Test void dragAndReleaseKeepNativeButtonAndFractionalMovement() {
        var nativeWidget = new NativeWidget();
        nativeWidget.accepted = true;
        var event = new MouseButtonEvent(-5.5, 10.75, 2);
        assertTrue(LegacyInput.mouseDragged(nativeWidget, event, -0.25, 3.75));
        assertEquals(List.of("drag", -5.5, 10.75, 2, -0.25, 3.75), nativeWidget.received);
        nativeWidget.accepted = false;
        assertFalse(LegacyInput.mouseReleased(nativeWidget, event));
        assertEquals(List.of("release", -5.5, 10.75, 2), nativeWidget.received);
    }

    @Test void nativeKeyboardAndCharacterCallbacksReceiveModifiersAndScancodes() {
        var nativeWidget = new NativeWidget();
        nativeWidget.accepted = true;
        var event = new KeyEvent(258, 37, 0xA5);
        assertTrue(LegacyInput.keyPressed(nativeWidget, event));
        assertEquals(List.of("key", 258, 37, 0xA5), nativeWidget.received);
        nativeWidget.accepted = false;
        assertFalse(LegacyInput.keyReleased(nativeWidget, event));
        assertEquals(List.of("key-release", 258, 37, 0xA5), nativeWidget.received);
        assertFalse(LegacyInput.charTyped(nativeWidget, new CharacterEvent('\u03BB', 3)));
        assertEquals(List.of("character", '\u03BB', 3), nativeWidget.received);
    }

    @Test void unknownKeyUsesTheInstalledMinecraftScancodeKeyType() {
        var key = LegacyInput.key(new KeyEvent(-1, 77, 0));
        assertEquals(InputConstants.Type.SCANCODE, key.getType());
        assertEquals(77, key.getValue());
        var namedKey = LegacyInput.key(new KeyEvent(258, 77, 0));
        assertEquals(InputConstants.Type.KEYSYM, namedKey.getType());
        assertEquals(258, namedKey.getValue());
    }
}
