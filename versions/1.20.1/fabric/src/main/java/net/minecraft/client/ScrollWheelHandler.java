package com.salts_inventory_update.client.input;

import org.joml.Vector2i;

public final class ScrollWheelHandler {
    public Vector2i onMouseScroll(double scrollX, double scrollY) {
        return new Vector2i((int) Math.signum(scrollX), (int) Math.signum(scrollY));
    }

    public static int getNextScrollWheelSelection(int direction, int selected, int count) {
        if (count <= 0) {
            return selected;
        }
        int next = selected - Integer.signum(direction);
        next %= count;
        if (next < 0) {
            next += count;
        }
        return next;
    }
}
