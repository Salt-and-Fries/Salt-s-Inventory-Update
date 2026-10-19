package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DesktopMouseButtonsTest {
    @Test
    void keepsNativeGlfwContainerClickButtons() {
        assertEquals(0, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
        assertEquals(1, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        assertEquals(0, DesktopMouseButtons.menuButton(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT));
        assertEquals(1, DesktopMouseButtons.menuButton(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT));
        assertThrows(IllegalArgumentException.class, () -> DesktopMouseButtons.menuButton(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
    }
}
