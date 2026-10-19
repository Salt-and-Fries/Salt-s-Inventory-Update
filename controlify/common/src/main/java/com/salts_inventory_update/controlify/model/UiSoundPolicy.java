package com.salts_inventory_update.controlify.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Coalesces changed UI actions into one fresh, rate-limited cue per client update. */
public final class UiSoundPolicy {
    public static final long NAVIGATION_INTERVAL_MILLIS = 200;
    public static final long QUIET_INTERVAL_MILLIS = 90;
    public static final long RESULT_INTERVAL_MILLIS = 90;
    public static final long MAX_PENDING_AGE_MILLIS = 100;

    /** Later families replace earlier ones in the same update, regardless of submission order. */
    public enum Priority { NAVIGATION, STRUCTURE, RESULT }

    /**
     * IDs describe stable actions, never item names, slot indices, or other changing state.
     * A zero-repeat NAVIGATION cue explicitly requests restarting feedback on every changed selection.
     */
    public record Cue(String id, Priority priority, long repeatMillis) {
        public Cue {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("A UI sound needs a stable action ID");
            Objects.requireNonNull(priority, "priority");
            if (repeatMillis < 0) throw new IllegalArgumentException("Negative UI sound repeat interval");
        }
    }

    private final Map<String, Long> lastKinds = new HashMap<>();
    private Long lastObserved;
    private Long lastAny;
    private Long lastNavigation;
    private Long lastResult;
    private Priority lastPriority;
    private record Candidate(Cue cue, long submittedAt, long sequence) { }
    private final Map<String, Candidate> pending = new HashMap<>();
    private long sequence;

    /**
     * Collects the latest changed action per stable ID for this update. Admission is evaluated at
     * drain time, so the delay between input and tick does not halve a steady five-per-second sweep.
     * A true result means collected, not that audio is eligible or has already played.
     */
    public boolean submit(Cue cue, long nowMillis) {
        Objects.requireNonNull(cue, "cue");
        observe(nowMillis);
        expirePending(nowMillis);
        pending.put(cue.id(), new Candidate(cue, nowMillis, sequence++));
        return true;
    }

    /** Consumes this update's winner. Rejected and replaced cues are discarded, never delayed. */
    public Optional<Cue> drain(long nowMillis) {
        observe(nowMillis);
        expirePending(nowMillis);
        Candidate selected = null;
        for (Candidate candidate : pending.values()) {
            if (!eligible(candidate.cue(), nowMillis)) continue;
            if (selected == null || candidate.cue().priority().ordinal() > selected.cue().priority().ordinal()
                || candidate.cue().priority() == selected.cue().priority() && candidate.sequence() > selected.sequence())
                selected = candidate;
        }
        pending.clear();
        sequence = 0;
        if (selected == null) return Optional.empty();
        Cue winner = selected.cue();
        lastKinds.put(winner.id(), nowMillis);
        lastAny = nowMillis;
        lastPriority = winner.priority();
        if (winner.priority() == Priority.NAVIGATION) lastNavigation = nowMillis;
        if (winner.priority() == Priority.RESULT) lastResult = nowMillis;
        return Optional.of(winner);
    }

    /** Mute, disconnect, and screen teardown discard pending audio as well as old cooldowns. */
    public void reset() {
        lastKinds.clear();
        lastObserved = null;
        lastAny = null;
        lastNavigation = null;
        lastResult = null;
        lastPriority = null;
        pending.clear();
        sequence = 0;
    }

    private boolean eligible(Cue cue, long nowMillis) {
        if (!elapsed(lastKinds.get(cue.id()), cue.repeatMillis(), nowMillis)) return false;
        if (cue.priority() == Priority.RESULT)
            return elapsed(lastResult, RESULT_INTERVAL_MILLIS, nowMillis);
        // Fast wheel/scroll selection replaces the preceding movement cue. Results and structural
        // changes retain their short ownership gap so navigation cannot immediately obscure them.
        if (cue.priority() == Priority.NAVIGATION && cue.repeatMillis() == 0)
            return lastPriority == Priority.NAVIGATION || elapsed(lastAny, QUIET_INTERVAL_MILLIS, nowMillis);
        if (!elapsed(lastAny, QUIET_INTERVAL_MILLIS, nowMillis)) return false;
        return cue.priority() != Priority.NAVIGATION
            || elapsed(lastNavigation, NAVIGATION_INTERVAL_MILLIS, nowMillis);
    }

    private static boolean elapsed(Long last, long interval, long now) {
        // The rollback check in observe() guarantees ordering. Compare without subtraction overflow.
        return last == null || (now >= last && (now - last < 0 || now - last >= interval));
    }

    private void observe(long nowMillis) {
        if (lastObserved != null && nowMillis < lastObserved) reset();
        lastObserved = nowMillis;
    }

    private void expirePending(long nowMillis) {
        pending.values().removeIf(candidate -> !withinPendingLifetime(candidate, nowMillis));
    }

    private static boolean withinPendingLifetime(Candidate candidate, long nowMillis) {
        long elapsed = nowMillis - candidate.submittedAt();
        return elapsed >= 0 && elapsed <= MAX_PENDING_AGE_MILLIS;
    }
}
