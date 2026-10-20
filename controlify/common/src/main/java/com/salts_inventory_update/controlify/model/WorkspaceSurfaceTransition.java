package com.salts_inventory_update.controlify.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Crossfades icons and live panes while their shared placement moves, including rapid reversals. */
public final class WorkspaceSurfaceTransition<K> {
    private static final long DURATION_NANOS = 180_000_000L;
    private final Map<K, State> states = new HashMap<>();

    public double value(K id, boolean live, long now, boolean animate) {
        double target = live ? 1 : 0;
        State state = states.get(id);
        if (state == null || !animate) {
            states.put(id, new State(target, target, now));
            return target;
        }
        double sampled = state.sample(now);
        if (state.target != target) states.put(id, new State(sampled, target, now));
        return sampled;
    }

    public void retain(Set<K> ids) { states.keySet().retainAll(ids); }
    public void reset() { states.clear(); }

    private record State(double from, double target, long started) {
        double sample(long now) {
            double t = Math.max(0, Math.min(1, (now - started) / (double) DURATION_NANOS));
            return from + (target - from) * t * t * (3 - 2 * t);
        }
    }
}
