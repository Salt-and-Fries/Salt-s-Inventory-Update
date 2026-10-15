package com.salts_inventory_update.controlify.model;

/** One row per deliberate right-stick step; no fractional per-render wheel accumulation. */
public final class ControllerScrollRepeat {
    private int direction;
    private long nextRepeat;

    public int update(double vertical, long nowMillis) {
        int nextDirection = Math.abs(vertical) < 0.55 ? 0 : vertical > 0 ? 1 : -1;
        if (nextDirection == 0) { reset(); return 0; }
        if (nextDirection != direction) {
            direction = nextDirection;
            nextRepeat = nowMillis + 350;
            return direction;
        }
        if (nowMillis < nextRepeat) return 0;
        nextRepeat = nowMillis + (Math.abs(vertical) >= 0.9 ? 160 : 230);
        return direction;
    }

    public void reset() { direction = 0; nextRepeat = 0; }
}
