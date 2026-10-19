package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Retargetable smooth movement, scaling, appearance and highlight transitions for every window. */
public final class WorkspaceAnimator<K> {
    private WorkspaceLayout<K> from;
    private WorkspaceLayout<K> target;
    private long startedNanos;
    private long durationMillis;

    public WorkspaceLayout<K> animate(WorkspaceLayout<K> next, long nowNanos,
                                       boolean animationsEnabled, long transitionMillis) {
        Objects.requireNonNull(next, "next");
        if (transitionMillis < 0) {
            throw new IllegalArgumentException("Animation duration must be nonnegative");
        }
        if (!animationsEnabled || transitionMillis == 0) {
            from = next;
            target = next;
            startedNanos = nowNanos;
            durationMillis = 0;
            return next;
        }
        if (!next.equals(target)) {
            WorkspaceLayout<K> current = target == null ? initial(next) : sample(nowNanos);
            from = origins(current, next);
            target = next;
            startedNanos = nowNanos;
            durationMillis = transitionMillis;
        }
        return sample(nowNanos);
    }

    public void reset() {
        from = null;
        target = null;
        durationMillis = 0;
    }

    private WorkspaceLayout<K> sample(long nowNanos) {
        if (durationMillis == 0) {
            return target;
        }
        double progress = Math.max(0, Math.min(1,
                ((double) nowNanos - startedNanos) / (durationMillis * 1_000_000.0)));
        if (progress >= 1) {
            return target;
        }
        double eased = progress * progress * (3.0 - 2.0 * progress);
        Map<K, WindowPlacement> result = new LinkedHashMap<>();
        from.windows().forEach((id, origin) -> {
            WindowPlacement destination = target.windows().get(id);
            if (destination == null) {
                destination = origin.translate(0, -20).withOpacity(0);
            }
            result.put(id, WindowPlacement.interpolate(origin, destination, eased));
        });
        return new WorkspaceLayout<>(result, WindowPlacement.interpolate(from.tuck(), target.tuck(), eased));
    }

    private WorkspaceLayout<K> initial(WorkspaceLayout<K> next) {
        Map<K, WindowPlacement> windows = new LinkedHashMap<>();
        next.windows().forEach((id, placement) -> windows.put(id, placement.translate(0, -20).withOpacity(0)));
        return new WorkspaceLayout<>(windows, next.tuck().withOpacity(0));
    }

    private WorkspaceLayout<K> origins(WorkspaceLayout<K> current, WorkspaceLayout<K> next) {
        Map<K, WindowPlacement> result = new LinkedHashMap<>(current.windows());
        next.windows().forEach((id, placement) -> result.putIfAbsent(id, placement.translate(0, -20).withOpacity(0)));
        return new WorkspaceLayout<>(result, current.tuck());
    }
}
