package com.salts_inventory_update.api.client.desktop;

import com.mojang.blaze3d.platform.InputConstants;

/** SDL mouse button IDs differ from the primary/secondary IDs used by container menus. */
public final class DesktopMouseButtons {
    private DesktopMouseButtons() {
    }

    public static int menuButton(int mouseButton) {
        return switch (mouseButton) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            default -> throw new IllegalArgumentException("Unsupported container mouse button: " + mouseButton);
        };
    }
}
