package com.salts_inventory_update.controlify.model;

/** Reversible, bounded opening motion; input ownership is deliberately kept outside this model. */
public final class ContextualPresentationTransition {
    public static final long DURATION_NANOS = 260_000_000L;
    private double from, target;
    private long startedAt, duration;

    public void target(boolean open, long nowNanos, boolean animated) {
        double next = open ? 1 : 0;
        if (next == target && (animated || from == target)) return;
        double current = value(nowNanos);
        target = next; from = animated ? current : next; startedAt = nowNanos;
        duration = animated ? Math.round(DURATION_NANOS * Math.abs(next - current)) : 0;
    }
    public double value(long nowNanos) {
        if (duration == 0) return target;
        double t = Math.max(0, Math.min(1, (nowNanos - startedAt) / (double) duration));
        double eased = t * t * (3 - 2 * t);
        return from + (target - from) * eased;
    }
    public boolean finished(long nowNanos) { return duration == 0 || nowNanos - startedAt >= duration; }
    public void reset() { from = target = 0; startedAt = duration = 0; }
}
