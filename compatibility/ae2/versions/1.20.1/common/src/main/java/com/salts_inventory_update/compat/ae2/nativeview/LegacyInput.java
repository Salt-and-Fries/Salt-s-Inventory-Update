package com.salts_inventory_update.compat.ae2.nativeview;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.input.CharacterEvent;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/** Signature bridge to Minecraft 1.20.1's original widget input handlers. */
public final class LegacyInput {
    private LegacyInput() { }

    public static boolean mouseClicked(GuiEventListener widget, MouseButtonEvent event, boolean doubleClick) {
        // This native widget API determines repeated clicks from its own state.
        return widget.mouseClicked(event.x(), event.y(), event.button());
    }

    public static boolean mouseReleased(GuiEventListener widget, MouseButtonEvent event) {
        return widget.mouseReleased(event.x(), event.y(), event.button());
    }

    public static boolean mouseDragged(GuiEventListener widget, MouseButtonEvent event, double dx, double dy) {
        return widget.mouseDragged(event.x(), event.y(), event.button(), dx, dy);
    }

    public static boolean keyPressed(GuiEventListener widget, KeyEvent event) {
        return widget.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public static boolean keyReleased(GuiEventListener widget, KeyEvent event) {
        return widget.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    public static boolean charTyped(GuiEventListener widget, CharacterEvent event) {
        return widget.charTyped(event.codepoint(), event.modifiers());
    }

    public static InputConstants.Key key(KeyEvent event) {
        return InputConstants.getKey(event.key(), event.scancode());
    }

    public static boolean matches(KeyMapping mapping, KeyEvent event) {
        return mapping.matches(event.key(), event.scancode());
    }

    public static MouseButtonEvent leftButton(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x(), event.y(), 0);
    }

    public static boolean hasShiftDown(Minecraft minecraft) {
        return Screen.hasShiftDown();
    }
}
