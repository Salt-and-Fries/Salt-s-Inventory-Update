package com.salts_inventory_update.controlify.model;

/** Elapsed-time analog scrolling with a deliberate engagement threshold and bounded catchup. */
public final class ControllerScrollRepeat {
    public static final double ACTIVATION_THRESHOLD = .55;
    public static final double RELEASE_THRESHOLD = .45;
    public static final double MIN_ROWS_PER_SECOND = 4;
    public static final double MAX_ROWS_PER_SECOND = 24;
    public static final long MAX_ELAPSED_MILLIS = 100;
    private int direction;
    private long updatedAt;
    private double previousRate;
    private double fractionalRows;

    /** Returns signed rows for this update; callers dispatch each row through normal scrolling. */
    public int update(double vertical, long nowMillis) {
        if (!Double.isFinite(vertical)) { reset(); return 0; }
        double magnitude = Math.min(1, Math.abs(vertical));
        if (magnitude <= RELEASE_THRESHOLD) { reset(); return 0; }
        int nextDirection = vertical > 0 ? 1 : -1;
        if (direction != 0 && nowMillis < updatedAt) reset();
        if (nextDirection != direction) {
            reset();
            if (magnitude < ACTIVATION_THRESHOLD) return 0;
            direction = nextDirection;
            updatedAt = nowMillis;
            previousRate = rowsPerSecond(magnitude);
            return direction;
        }
        long elapsed = nowMillis - updatedAt;
        // A stalled frame discards the excess time instead of replaying old scrolling afterward.
        elapsed = elapsed < 0 ? MAX_ELAPSED_MILLIS : Math.min(elapsed, MAX_ELAPSED_MILLIS);
        double rate = rowsPerSecond(magnitude);
        fractionalRows += elapsed * (previousRate + rate) / 2000.0;
        updatedAt = nowMillis;
        previousRate = rate;
        int rows = (int) Math.floor(fractionalRows + 1e-9);
        fractionalRows = Math.max(0, fractionalRows - rows);
        return direction * rows;
    }

    private static double rowsPerSecond(double magnitude) {
        double deflection = Math.max(0, Math.min(1, (magnitude - ACTIVATION_THRESHOLD) / (1 - ACTIVATION_THRESHOLD)));
        double eased = deflection * deflection * (3 - 2 * deflection);
        return MIN_ROWS_PER_SECOND + (MAX_ROWS_PER_SECOND - MIN_ROWS_PER_SECOND) * eased;
    }

    public void reset() { direction = 0; updatedAt = 0; previousRate = 0; fractionalRows = 0; }
}
