package com.salts_inventory_update.client;

/** Pixel-aligned pull-down geometry, independent of the renderer and game clock. */
final class DesktopTabState {
    static final int WIDTH = 96;
    static final int HEIGHT = 32;
    static final int PEEK = 3;
    private double revealed;
    private long lastTime;

    /** Hiding the desktop tab must retain both keyboard close-all and native Return access. */
    static boolean shouldShow(boolean configured, boolean inventoryHeld, boolean nativeView) {
        return configured || inventoryHeld || nativeView;
    }

    void reset() { revealed = 0; lastTime = 0; }

    int x(int screenWidth) { return (screenWidth - WIDTH) / 2; }
    int y() { return (int)Math.round(revealed) - HEIGHT + PEEK; }

    boolean contains(int screenWidth, double mouseX, double mouseY) {
        return mouseX >= x(screenWidth) && mouseX < x(screenWidth) + WIDTH
            && mouseY >= 0 && mouseY < y() + HEIGHT;
    }

    void update(long now, int screenWidth, double mouseX, double mouseY, boolean forced) {
        long delta = lastTime == 0 ? 0 : Math.max(0, Math.min(50, now - lastTime));
        lastTime = now;
        double target = forced || contains(screenWidth, mouseX, mouseY) ? HEIGHT - PEEK : 0;
        double step = delta * (HEIGHT - PEEK) / 160.0;
        revealed += Math.copySign(Math.min(Math.abs(target - revealed), step), target - revealed);
    }
}
