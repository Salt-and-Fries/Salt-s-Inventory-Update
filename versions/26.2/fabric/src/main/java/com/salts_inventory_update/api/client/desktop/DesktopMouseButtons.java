package com.salts_inventory_update.api.client.desktop;
/** Native GLFW primary/secondary IDs follow container-menu conventions. */
public final class DesktopMouseButtons {
    private DesktopMouseButtons() { }
    public static int menuButton(int button) {
        if (button != 0 && button != 1) throw new IllegalArgumentException("Unsupported container mouse button: " + button);
        return button;
    }
}
