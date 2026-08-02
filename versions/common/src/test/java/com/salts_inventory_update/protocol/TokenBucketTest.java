package com.salts_inventory_update.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenBucketTest {
    @Test
    void enforcesBurstAndRefill() {
        TokenBucket bucket = new TokenBucket(2.0D, 2.0D, 1_000L);
        assertTrue(bucket.tryConsume(1_000L));
        assertTrue(bucket.tryConsume(1_000L));
        assertFalse(bucket.tryConsume(1_000L));
        assertTrue(bucket.tryConsume(500_001_000L));
        assertFalse(bucket.tryConsume(500_001_000L));
        assertEquals(2.0D, bucket.available(2_500_001_000L), 1.0E-9D);
    }

    @Test
    void backwardClockCannotMintTokens() {
        TokenBucket bucket = new TokenBucket(1.0D, 1.0D, 100L);
        assertTrue(bucket.tryConsume(100L));
        assertFalse(bucket.tryConsume(99L));
    }

    @Test
    void rejectsNonFiniteConfigurationAndConsumption() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(Double.POSITIVE_INFINITY, 1.0D, 0L));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1.0D, Double.POSITIVE_INFINITY, 0L));

        TokenBucket bucket = new TokenBucket(1.0D, 1.0D, 0L);
        assertThrows(IllegalArgumentException.class, () -> bucket.tryConsume(Double.NaN, 0L));
        assertThrows(IllegalArgumentException.class, () -> bucket.tryConsume(Double.POSITIVE_INFINITY, 0L));
    }

    @Test
    void monotonicClockWrapStillRefills() {
        TokenBucket bucket = new TokenBucket(1_000_000_000.0D, 2.0D, Long.MAX_VALUE - 1L);
        assertTrue(bucket.tryConsume(Long.MAX_VALUE - 1L));
        assertTrue(bucket.tryConsume(Long.MAX_VALUE - 1L));
        assertTrue(bucket.tryConsume(Long.MIN_VALUE));
    }

    @Test
    void tinyFractionCannotBeConsumedRepeatedlyThroughAbsoluteTolerance() {
        TokenBucket bucket = new TokenBucket(1.0E-12D, 1.0E-12D, 0L);
        assertTrue(bucket.tryConsume(1.0E-12D, 0L));
        assertFalse(bucket.tryConsume(1.0E-12D, 0L));
        assertFalse(bucket.tryConsume(1.0E-12D, 0L));
        assertEquals(0.0D, bucket.available(0L));
    }
}
