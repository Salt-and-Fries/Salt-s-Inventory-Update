package com.salts_inventory_update.controlify.model;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Tracks a visible destination increase after a craft request, without interpreting dispatch as
 * success or inventing a failure when no change arrives. This is an observation, not a server ack.
 */
public final class ObservedCraftFeedback {
    public static final long TIMEOUT_MILLIS = 2500;

    public record Identity(String actionId, UUID sourceId, UUID destinationId) {
        public Identity {
            if (actionId == null || actionId.isBlank())
                throw new IllegalArgumentException("A craft observation needs an action ID");
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(destinationId, "destinationId");
        }
    }

    public enum Result { NONE, COMPLETE }

    private Identity pendingIdentity;
    private long baselineCount;
    private long minimumOutput;
    private long startedAt;
    private Long lastObserved;

    /**
     * Arms one accepted request using a count captured before dispatch. Additional requests while
     * pending share this observation: they do not replace its item filter or extend its timeout.
     */
    public boolean arm(Identity identity, long baselineCount, long minimumOutput, long nowMillis) {
        Objects.requireNonNull(identity, "identity");
        if (baselineCount < 0) throw new IllegalArgumentException("Negative destination item count");
        if (minimumOutput <= 0) throw new IllegalArgumentException("Craft output must be positive");
        observe(nowMillis);
        expire(nowMillis);
        if (pending()) return false;
        pendingIdentity = identity;
        this.baselineCount = baselineCount;
        this.minimumOutput = minimumOutput;
        startedAt = nowMillis;
        return true;
    }

    /**
     * The caller keeps the original same-item-and-components filter and validates both native
     * window lifetimes. A missing snapshot waits silently; invalid identity or lifetime cancels.
     */
    public Result update(Identity identity, OptionalLong matchingCount, boolean lifetimesValid, long nowMillis) {
        Objects.requireNonNull(matchingCount, "matchingCount");
        if (!observe(nowMillis)) return Result.NONE;
        expire(nowMillis);
        if (!pending()) return Result.NONE;
        if (!lifetimesValid || !pendingIdentity.equals(identity)) {
            pendingIdentity = null;
            return Result.NONE;
        }
        if (matchingCount.isEmpty() || matchingCount.getAsLong() < baselineCount) return Result.NONE;
        // Both values are nonnegative, so subtraction cannot overflow even near Long.MAX_VALUE.
        if (matchingCount.getAsLong() - baselineCount < minimumOutput) return Result.NONE;
        pendingIdentity = null;
        return Result.COMPLETE;
    }

    public boolean pending() { return pendingIdentity != null; }

    /** Workspace/world teardown leaves no feedback to replay in a different session. */
    public void reset() {
        pendingIdentity = null;
        lastObserved = null;
    }

    private boolean observe(long nowMillis) {
        boolean monotonic = lastObserved == null || nowMillis >= lastObserved;
        if (!monotonic) pendingIdentity = null;
        lastObserved = nowMillis;
        return monotonic;
    }

    private void expire(long nowMillis) {
        if (!pending()) return;
        long age = nowMillis - startedAt;
        if (age < 0 || age > TIMEOUT_MILLIS) pendingIdentity = null;
    }
}
