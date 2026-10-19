package com.salts_inventory_update.controlify.model;

import java.util.HashMap;
import java.util.Map;

/** Admission timing for short UI cues; rejected repeats never postpone the next useful cue. */
public final class UiSoundGate {
    public static final long GLOBAL_INTERVAL_MILLIS = 45;
    private final Map<String, Long> lastKinds = new HashMap<>();
    private long lastAny = Long.MIN_VALUE;

    public boolean allow(String kind, long intervalMillis, long nowMillis) {
        if (intervalMillis < 0) throw new IllegalArgumentException("Negative UI sound interval");
        if (lastAny != Long.MIN_VALUE && nowMillis < lastAny) { lastKinds.clear(); lastAny = Long.MIN_VALUE; }
        if (lastAny != Long.MIN_VALUE && nowMillis - lastAny < GLOBAL_INTERVAL_MILLIS) return false;
        Long last = lastKinds.get(kind);
        if (last != null && nowMillis - last < intervalMillis) return false;
        lastKinds.put(kind, nowMillis); lastAny = nowMillis;
        return true;
    }
}
