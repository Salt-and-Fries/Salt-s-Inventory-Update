package com.salts_inventory_update.client.input;

import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return (this.modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || Screen.hasShiftDown();
    }

    public boolean hasControlDown() {
        return (this.modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || Screen.hasControlDown();
    }

    public boolean hasAltDown() {
        return (this.modifiers & GLFW.GLFW_MOD_ALT) != 0 || Screen.hasAltDown();
    }

    public boolean isEscape() {
        return this.key == GLFW.GLFW_KEY_ESCAPE;
    }
}
