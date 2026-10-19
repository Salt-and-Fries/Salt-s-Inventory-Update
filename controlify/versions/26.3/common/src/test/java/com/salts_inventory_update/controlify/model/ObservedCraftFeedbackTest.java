package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;

import static com.salts_inventory_update.controlify.model.ObservedCraftFeedback.Result.*;
import static org.junit.jupiter.api.Assertions.*;

final class ObservedCraftFeedbackTest {
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DESTINATION = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final ObservedCraftFeedback.Identity CRAFT = identity("craft", SOURCE, DESTINATION);

    @Test void dispatchOrUnchangedDestinationNeverImpliesSuccess() {
        var feedback = new ObservedCraftFeedback();
        assertTrue(feedback.arm(CRAFT, 20, 4, 1000));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(20), true, 1000));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(20), true, 1100));
        assertTrue(feedback.pending());
    }

    @Test void outputNeedsAtLeastOneCompleteNativeRecipeBatch() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 20, 4, 1000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(23), true, 1050));
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(24), true, 1100));
        assertFalse(feedback.pending());
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(28), true, 1150));
    }

    @Test void largerStackCraftCompletesOnceWithoutRequiringAFinalStackSize() {
        var feedback = new ObservedCraftFeedback();
        var stack = identity("craft_stack", SOURCE, DESTINATION);
        assertTrue(feedback.arm(stack, 56, 4, 1000));
        assertEquals(COMPLETE, feedback.update(stack, OptionalLong.of(64), true, 1050));
        assertEquals(NONE, feedback.update(stack, OptionalLong.of(64), true, 1100));
    }

    @Test void missingOrDecreasedDestinationDoesNotComplete() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 20, 4, 1000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.empty(), true, 1050));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(-1), true, 1100));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(19), true, 1150));
        assertTrue(feedback.pending());
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(24), true, 1200));
    }

    @Test void furtherAttemptsDoNotReplaceBaselineOrItemIdentityOrExtendTimeout() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 20, 4, 1000);
        assertFalse(feedback.arm(identity("craft_stack", SOURCE, DESTINATION), 100, 1, 2000));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(23), true, 3499));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(24), true, 3501));
        assertFalse(feedback.pending());
    }

    @Test void coalescedAttemptsStillProduceOnlyTheOriginalCompletion() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 20, 4, 1000);
        assertFalse(feedback.arm(CRAFT, 20, 4, 1050));
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(28), true, 1100));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(28), true, 1150));
    }

    @Test void timeoutBoundaryAcceptsFreshObservationAndExpiresSilentlyAfterward() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 0, 1, 1000);
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(1), true, 3500));
        feedback.arm(CRAFT, 0, 1, 4000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(1), true, 6501));
        assertFalse(feedback.pending());
    }

    @Test void timedOutAttemptDoesNotBlockANewAcceptedRequest() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 0, 4, 1000);
        assertTrue(feedback.arm(CRAFT, 100, 4, 3501));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(4), true, 3550));
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(104), true, 3600));
    }

    @Test void staleSourceDestinationOrActionCannotCompleteANewWindowSession() {
        var feedback = new ObservedCraftFeedback();
        for (var changed : new ObservedCraftFeedback.Identity[] {
                identity("craft_stack", SOURCE, DESTINATION),
                identity("craft", OTHER, DESTINATION),
                identity("craft", SOURCE, OTHER) }) {
            feedback.arm(CRAFT, 0, 1, 1000);
            assertEquals(NONE, feedback.update(changed, OptionalLong.of(1), true, 1050));
            assertFalse(feedback.pending());
            assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(1), true, 1100));
        }
    }

    @Test void invalidLifetimesAndWorkspaceResetCancelWithoutARefusal() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 0, 1, 1000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(1), false, 1050));
        assertFalse(feedback.pending());
        feedback.arm(CRAFT, 0, 1, 1100);
        feedback.reset();
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(1), true, 1150));
        assertFalse(feedback.pending());
    }

    @Test void clockRollbackDiscardsOldWatchAndAllowsOnlyAFreshAttempt() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, 0, 1, 1000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(0), true, 1100));
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(1), true, 1050));
        assertFalse(feedback.pending());
        assertTrue(feedback.arm(CRAFT, 0, 1, 10));
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(1), true, 20));
    }

    @Test void largeCountsCannotOverflowIntoFalseCompletion() {
        var feedback = new ObservedCraftFeedback();
        feedback.arm(CRAFT, Long.MAX_VALUE - 2, 4, 1000);
        assertEquals(NONE, feedback.update(CRAFT, OptionalLong.of(Long.MAX_VALUE), true, 1050));
        feedback.reset();
        feedback.arm(CRAFT, Long.MAX_VALUE - 4, 4, 2000);
        assertEquals(COMPLETE, feedback.update(CRAFT, OptionalLong.of(Long.MAX_VALUE), true, 2050));
    }

    @Test void invalidAttemptDefinitionsAreRejected() {
        var feedback = new ObservedCraftFeedback();
        assertThrows(IllegalArgumentException.class, () -> identity(" ", SOURCE, DESTINATION));
        assertThrows(NullPointerException.class, () -> identity("craft", null, DESTINATION));
        assertThrows(NullPointerException.class, () -> identity("craft", SOURCE, null));
        assertThrows(NullPointerException.class, () -> feedback.arm(null, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback.arm(CRAFT, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback.arm(CRAFT, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> feedback.arm(CRAFT, 0, -1, 0));
        assertFalse(feedback.pending());
    }

    private static ObservedCraftFeedback.Identity identity(String action, UUID source, UUID destination) {
        return new ObservedCraftFeedback.Identity(action, source, destination);
    }
}
