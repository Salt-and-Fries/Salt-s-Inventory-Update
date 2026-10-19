package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CursorWarpEchoTrackerTest {
    private static final long WINDOW = 17L;
    private static final long START = 1_000_000_000L;

    @Test void eachProgrammaticWarpConsumesAtMostOneMatchingCallback() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object();
        tracker.record(owner, WINDOW, 20, 30, START);
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 1));
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 2));
        tracker.record(owner, WINDOW, 20, 30, START + 3);
        tracker.record(owner, WINDOW, 20, 30, START + 3);
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 4));
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 5));
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 6));
    }

    @Test void ownerIdentityAndWindowMustMatchWithoutInvalidatingOtherScopes() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new ArrayList<>(), equalButDifferentOwner = new ArrayList<>();
        assertEquals(owner, equalButDifferentOwner);
        assertNotSame(owner, equalButDifferentOwner);
        tracker.record(owner, WINDOW, 20, 30, START);
        assertFalse(tracker.consume(equalButDifferentOwner, WINDOW, 20, 30, START + 1));
        assertFalse(tracker.consume(owner, WINDOW + 1, 20, 30, START + 2));
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 3));
    }

    @Test void differentPhysicalMovementClearsOnlyItsOwnersWindowReceipts() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object(), otherOwner = new Object();
        tracker.record(owner, WINDOW, 20, 30, START);
        tracker.record(owner, WINDOW, 40, 50, START);
        tracker.record(owner, WINDOW + 1, 20, 30, START);
        tracker.record(otherOwner, WINDOW, 20, 30, START);
        assertFalse(tracker.consume(owner, WINDOW, 21, 30, START + 1));
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 2), "A real move must not suppress a later return");
        assertFalse(tracker.consume(owner, WINDOW, 40, 50, START + 3));
        assertTrue(tracker.consume(owner, WINDOW + 1, 20, 30, START + 4));
        assertTrue(tracker.consume(otherOwner, WINDOW, 20, 30, START + 5));
    }

    @Test void matchesActualOsRoundedPositionExactlyRatherThanRequestedCoordinatesOrATolerance() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object();
        var rounded = tracker.record(owner, WINDOW, 12.6, 20.2, START);
        rounded.actualPosition(13, 20);
        assertTrue(tracker.consume(owner, WINDOW, 13, 20, START + 1));
        tracker.record(owner, WINDOW, 12.6, 20.2, START + 2).actualPosition(13, 20);
        assertFalse(tracker.consume(owner, WINDOW, 12.6, 20.2, START + 3));
        tracker.record(owner, WINDOW, 12.6, 20.2, START + 4).actualPosition(13, 20);
        assertFalse(tracker.consume(owner, WINDOW, Math.nextUp(13d), 20, START + 5));
    }

    @Test void receiptsExpireAfterTwoHundredMillisecondsAndCannotMatchBeforeTheirCreation() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object();
        tracker.record(owner, WINDOW, 20, 30, START);
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 200_000_000L));
        tracker.record(owner, WINDOW, 20, 30, START);
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 200_000_001L));
        tracker.record(owner, WINDOW, 20, 30, START);
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START - 1));
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 1));
    }

    @Test void queueRetainsOnlyTheEightMostRecentWarpReceipts() {
        var tracker = new CursorWarpEchoTracker();
        Object[] owners = new Object[9];
        for (int i = 0; i < owners.length; i++) {
            owners[i] = new Object();
            tracker.record(owners[i], WINDOW, 20 + i, 30, START + i);
        }
        assertFalse(tracker.consume(owners[0], WINDOW, 20, 30, START + 10));
        for (int i = 1; i < owners.length; i++)
            assertTrue(tracker.consume(owners[i], WINDOW, 20 + i, 30, START + 10), "Receipt " + i + " remains available");
    }

    @Test void discardingAnUnperformedWarpPreservesOtherReceipts() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object();
        var failedWarp = tracker.record(owner, WINDOW, 20, 30, START);
        tracker.record(owner, WINDOW, 40, 50, START);
        tracker.discard(failedWarp);
        assertTrue(tracker.consume(owner, WINDOW, 40, 50, START + 1));
        assertFalse(tracker.consume(owner, WINDOW, 20, 30, START + 2));
    }

    @Test void invalidInputsCannotCreateOrPoisonAValidWarpReceipt() {
        var tracker = new CursorWarpEchoTracker();
        Object owner = new Object();
        assertNull(tracker.record(null, WINDOW, 20, 30, START));
        assertNull(tracker.record(owner, 0, 20, 30, START));
        assertNull(tracker.record(owner, WINDOW, Double.NaN, 30, START));
        assertNull(tracker.record(owner, WINDOW, 20, Double.POSITIVE_INFINITY, START));
        var valid = tracker.record(owner, WINDOW, 20, 30, START);
        valid.actualPosition(Double.NaN, 40);
        valid.actualPosition(40, Double.NEGATIVE_INFINITY);
        assertFalse(tracker.consume(owner, WINDOW, Double.NaN, 30, START + 1));
        assertTrue(tracker.consume(owner, WINDOW, 20, 30, START + 2));
    }
}
