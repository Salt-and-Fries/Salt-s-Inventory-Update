package com.salts_inventory_update.client;

/**
 * A window-relative region rendered or handled outside a Salt window's base rectangle.
 *
 * <p>This is an internal contract for bundled compatibility definitions. Coordinates are relative to the
 * window's top-left corner and may be negative. Visual and interactive flags are independent so callers can
 * describe decorative overhangs, invisible interaction targets, or the usual rendered controls. Stable
 * attached controls constrain placement while transient overlays can remain interactive without moving the
 * base window when they appear.</p>
 */
public record InternalDesktopWindowRegion(
    int x,
    int y,
    int width,
    int height,
    boolean visual,
    boolean interactive,
    boolean constrainsWindow
) {
    public InternalDesktopWindowRegion {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Desktop window region dimensions must be non-negative");
        }
    }

    public static InternalDesktopWindowRegion visual(int x, int y, int width, int height) {
        return new InternalDesktopWindowRegion(x, y, width, height, true, false, true);
    }

    public static InternalDesktopWindowRegion interactive(int x, int y, int width, int height) {
        return new InternalDesktopWindowRegion(x, y, width, height, false, true, true);
    }

    public static InternalDesktopWindowRegion visualAndInteractive(int x, int y, int width, int height) {
        return new InternalDesktopWindowRegion(x, y, width, height, true, true, true);
    }

    public static InternalDesktopWindowRegion transientVisualAndInteractive(
        int x,
        int y,
        int width,
        int height
    ) {
        return new InternalDesktopWindowRegion(x, y, width, height, true, true, false);
    }

    boolean contains(int windowX, int windowY, double mouseX, double mouseY) {
        return this.width > 0
            && this.height > 0
            && mouseX >= (double) windowX + this.x
            && mouseX < (double) windowX + this.x + this.width
            && mouseY >= (double) windowY + this.y
            && mouseY < (double) windowY + this.y + this.height;
    }
}
