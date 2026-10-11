package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;

/** Window-local GUI-pixel rectangle. Visual, hit and placement ownership are independent. */
public record DesktopWindowRegion(int x, int y, int width, int height, boolean visual,
                                  boolean interactive, boolean constrainsWindow, DesktopHitRole role) {
    public DesktopWindowRegion {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Region dimensions must be non-negative");
        Objects.requireNonNull(role, "role");
        Math.addExact(x, width);
        Math.addExact(y, height);
    }

    public static DesktopWindowRegion visual(int x, int y, int width, int height) {
        return new DesktopWindowRegion(x, y, width, height, true, false, true, DesktopHitRole.NONE);
    }

    public static DesktopWindowRegion interactive(int x, int y, int width, int height) {
        return new DesktopWindowRegion(x, y, width, height, false, true, true, DesktopHitRole.CONTENT);
    }

    public static DesktopWindowRegion visualAndInteractive(int x, int y, int width, int height) {
        return new DesktopWindowRegion(x, y, width, height, true, true, true, DesktopHitRole.CONTENT);
    }

    public static DesktopWindowRegion transientVisualAndInteractive(int x, int y, int width, int height) {
        return new DesktopWindowRegion(x, y, width, height, true, true, false, DesktopHitRole.POPUP);
    }

    public DesktopWindowRegion withRole(DesktopHitRole hitRole) {
        return new DesktopWindowRegion(x, y, width, height, visual, hitRole != DesktopHitRole.NONE, constrainsWindow, hitRole);
    }

    public DesktopWindowRegion transientRegion() {
        return new DesktopWindowRegion(x, y, width, height, visual, interactive, false, role);
    }

    public boolean contains(int windowX, int windowY, double mouseX, double mouseY) {
        return width > 0 && height > 0 && mouseX >= (double) windowX + x && mouseY >= (double) windowY + y
            && mouseX < (double) windowX + x + width && mouseY < (double) windowY + y + height;
    }
}
