package com.salts_inventory_update.protocol;

/** Thread-safe monotonic-time token bucket used to bound untrusted operations. */
public final class TokenBucket {
    private static final double NANOS_PER_SECOND = 1_000_000_000.0D;

    private final double refillPerNanosecond;
    private final double capacity;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(double tokensPerSecond, double burstCapacity, long nowNanos) {
        if (!Double.isFinite(tokensPerSecond)
            || !Double.isFinite(burstCapacity)
            || !(tokensPerSecond > 0.0D)
            || !(burstCapacity > 0.0D)
            || !(tokensPerSecond / NANOS_PER_SECOND > 0.0D)) {
            throw new IllegalArgumentException("token rate and burst capacity must be positive and finite");
        }
        this.refillPerNanosecond = tokensPerSecond / NANOS_PER_SECOND;
        this.capacity = burstCapacity;
        this.tokens = burstCapacity;
        this.lastRefillNanos = nowNanos;
    }

    public synchronized boolean tryConsume(long nowNanos) {
        return tryConsume(1.0D, nowNanos);
    }

    public synchronized boolean tryConsume(double amount, long nowNanos) {
        if (!Double.isFinite(amount) || !(amount > 0.0D) || amount > capacity) {
            throw new IllegalArgumentException("amount must be positive, finite, and no greater than capacity");
        }
        refill(nowNanos);
        if (tokens < amount) {
            return false;
        }
        tokens -= amount;
        return true;
    }

    public synchronized double available(long nowNanos) {
        refill(nowNanos);
        return tokens;
    }

    private void refill(long nowNanos) {
        long elapsedNanos = nowNanos - lastRefillNanos;
        if (elapsedNanos <= 0L) {
            return;
        }
        tokens = Math.min(capacity, tokens + elapsedNanos * refillPerNanosecond);
        lastRefillNanos = nowNanos;
    }
}
