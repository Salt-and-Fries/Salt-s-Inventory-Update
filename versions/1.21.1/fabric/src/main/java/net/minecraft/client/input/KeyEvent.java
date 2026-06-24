package com.salts_inventory_update.client.input;

import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }

    public boolean hasAltDown() {
        return Screen.hasAltDown();
    }

    public boolean isEscape() {
        return this.key == GLFW.GLFW_KEY_ESCAPE;
    }
}
