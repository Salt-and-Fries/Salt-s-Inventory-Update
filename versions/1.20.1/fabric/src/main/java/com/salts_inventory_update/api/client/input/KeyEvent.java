package com.salts_inventory_update.api.client.input;

import org.lwjgl.glfw.GLFW;

public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return (this.modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    public boolean hasControlDown() {
        return (this.modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    public boolean hasAltDown() {
        return (this.modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    public boolean isEscape() {
        return this.key == GLFW.GLFW_KEY_ESCAPE;
    }
}
