package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.salts_inventory_update.controlify.model.UiSoundPolicy.Priority.*;
import static org.junit.jupiter.api.Assertions.*;

final class UiSoundPolicyTest {
    private static final UiSoundPolicy.Cue NAVIGATE = cue("navigate", NAVIGATION, 200);
    private static final UiSoundPolicy.Cue FOCUS = cue("focus", NAVIGATION, 200);
    private static final UiSoundPolicy.Cue CAROUSEL = cue("carousel", NAVIGATION, 0);
    private static final UiSoundPolicy.Cue SCROLL = cue("scroll", NAVIGATION, 0);
    private static final UiSoundPolicy.Cue OPEN = cue("open", STRUCTURE, 160);
    private static final UiSoundPolicy.Cue CONFIRM = cue("confirm", RESULT, 120);
    private static final UiSoundPolicy.Cue REJECT = cue("reject", RESULT, 220);

    @Test void outcomeWinsOverNavigationAndStructureInEitherOrder() {
        var policy = new UiSoundPolicy();
        assertTrue(policy.submit(NAVIGATE, 1000));
        assertTrue(policy.submit(OPEN, 1000));
        assertTrue(policy.submit(CONFIRM, 1000));
        assertEquals(Optional.of(CONFIRM), policy.drain(1000));
        assertEquals(Optional.empty(), policy.drain(1000));

        policy.reset();
        assertTrue(policy.submit(REJECT, 1000));
        assertTrue(policy.submit(OPEN, 1000));
        assertTrue(policy.submit(FOCUS, 1000));
        assertEquals(Optional.of(REJECT), policy.drain(1000));
    }

    @Test void newerNavigationReplacesOlderSelectionWithoutCreatingAPlaybackQueue() {
        var policy = new UiSoundPolicy();
        assertTrue(policy.submit(NAVIGATE, 1000));
        assertTrue(policy.submit(FOCUS, 1010));
        assertEquals(Optional.of(FOCUS), policy.drain(1010));
        assertEquals(Optional.empty(), policy.drain(1210));
        // A discarded candidate never starts its per-action timer.
        assertTrue(policy.submit(cue("navigate", NAVIGATION, 1000), 1210));
        assertEquals("navigate", policy.drain(1210).orElseThrow().id());
    }

    @Test void rapidOrdinaryNavigationStaysAtFiveCuesPerSecondAcrossKinds() {
        var policy = new UiSoundPolicy();
        int emitted = 0;
        for (long now = 0; now < 1000; now++) {
            policy.submit(now % 2 == 0 ? NAVIGATE : FOCUS, now);
            if (policy.drain(now).isPresent()) emitted++;
        }
        assertEquals(5, emitted);
        assertTrue(policy.submit(FOCUS, 1000));
        assertEquals(Optional.of(FOCUS), policy.drain(1000));
    }

    @Test void steadyOrdinaryNavigationRemainsAudibleAtFivePerSecond() {
        var policy = new UiSoundPolicy();
        for (long now = 0; now <= 1000; now += 200) {
            assertTrue(policy.submit(FOCUS, now));
            assertEquals(Optional.of(FOCUS), policy.drain(now));
        }
    }

    @Test void inputToTickOffsetDoesNotHalveSteadyOrdinaryNavigationCadence() {
        var policy = new UiSoundPolicy();
        for (long input = 1000; input <= 2000; input += 200) {
            assertTrue(policy.submit(FOCUS, input));
            assertEquals(Optional.of(FOCUS), policy.drain(input + 20));
        }
    }

    @Test void rapidlyCollectedNavigationStillHasNoDelayedPlayback() {
        var policy = new UiSoundPolicy();
        assertTrue(policy.submit(FOCUS, 1000));
        assertEquals(Optional.of(FOCUS), policy.drain(1020));
        assertTrue(policy.submit(FOCUS, 1100));
        assertEquals(Optional.empty(), policy.drain(1120));
        assertEquals(Optional.empty(), policy.drain(1220));
        assertTrue(policy.submit(FOCUS, 1221));
        assertEquals(Optional.of(FOCUS), policy.drain(1240));
    }

    @Test void blockedNewerOutcomeDoesNotHideAnOlderEligibleOutcome() {
        var policy = new UiSoundPolicy();
        emit(policy, REJECT, 1000);
        assertTrue(policy.submit(CONFIRM, 1090));
        assertTrue(policy.submit(REJECT, 1091));
        assertEquals(Optional.of(CONFIRM), policy.drain(1100));
        assertEquals(Optional.empty(), policy.drain(1300));
    }

    @Test void staleHighPriorityCandidateCannotBorrowFreshLowerPriorityLifetime() {
        var policy = new UiSoundPolicy();
        assertTrue(policy.submit(CONFIRM, 1000));
        assertTrue(policy.submit(NAVIGATE, 1090));
        assertEquals(Optional.of(NAVIGATE), policy.drain(1101));
        assertEquals(Optional.empty(), policy.drain(1200));
    }

    @Test void resultImmediatelyAfterNavigationBypassesQuietCooldown() {
        var policy = new UiSoundPolicy();
        emit(policy, NAVIGATE, 1000);
        assertTrue(policy.submit(CONFIRM, 1001));
        assertEquals(Optional.of(CONFIRM), policy.drain(1001));
        assertTrue(policy.submit(OPEN, 1090));
        assertEquals(Optional.empty(), policy.drain(1090));
        assertTrue(policy.submit(OPEN, 1091));
        assertEquals(Optional.of(OPEN), policy.drain(1091));
    }

    @Test void blockedResultDoesNotSuppressAnotherEligibleCueOrMoveCooldown() {
        var policy = new UiSoundPolicy();
        emit(policy, REJECT, 1000);
        assertTrue(policy.submit(REJECT, 1219));
        assertTrue(policy.submit(FOCUS, 1219));
        assertEquals(Optional.of(FOCUS), policy.drain(1219));
        assertTrue(policy.submit(REJECT, 1220));
        assertEquals(Optional.of(REJECT), policy.drain(1220));
    }

    @Test void alternatingResultsHaveSharedFloorAndEachKeepsItsOwnRepeatLimit() {
        var policy = new UiSoundPolicy();
        emit(policy, CONFIRM, 1000);
        assertTrue(policy.submit(REJECT, 1089));
        assertEquals(Optional.empty(), policy.drain(1089));
        emit(policy, REJECT, 1090);
        assertTrue(policy.submit(CONFIRM, 1179));
        assertEquals(Optional.empty(), policy.drain(1179));
        emit(policy, CONFIRM, 1180);
        assertTrue(policy.submit(REJECT, 1309));
        assertEquals(Optional.empty(), policy.drain(1309));
        emit(policy, REJECT, 1310);
    }

    @Test void duplicateRequestsInOneUpdateEmitOnlyOnce() {
        var policy = new UiSoundPolicy();
        for (int i = 0; i < 20; i++) assertTrue(policy.submit(CONFIRM, 1000));
        assertEquals(Optional.of(CONFIRM), policy.drain(1000));
        assertEquals(Optional.empty(), policy.drain(1000));
        assertTrue(policy.submit(CONFIRM, 1119));
        assertEquals(Optional.empty(), policy.drain(1119));
        emit(policy, CONFIRM, 1120);
    }

    @Test void staleUndrainedCueCannotPlayAfterPauseOrBlockFreshLowerPriorityCue() {
        var policy = new UiSoundPolicy();
        assertTrue(policy.submit(CONFIRM, 1000));
        assertEquals(Optional.of(CONFIRM), policy.drain(1100));
        policy.reset();
        assertTrue(policy.submit(CONFIRM, 1000));
        assertEquals(Optional.empty(), policy.drain(1101));
        assertEquals(Optional.empty(), policy.drain(1500));
        assertTrue(policy.submit(CONFIRM, 1600));
        assertTrue(policy.submit(NAVIGATE, 1701));
        assertEquals(Optional.of(NAVIGATE), policy.drain(1701));
    }

    @Test void mutedOrClosedScreenResetDiscardsPendingAndOldTiming() {
        var policy = new UiSoundPolicy();
        emit(policy, REJECT, 1000);
        assertTrue(policy.submit(CONFIRM, 1100));
        policy.reset();
        assertEquals(Optional.empty(), policy.drain(1101));
        emit(policy, REJECT, 1101);
    }

    @Test void clockRollbackAlsoResetsWhenTheLastObservationWasAnUndrainedRequest() {
        var policy = new UiSoundPolicy();
        emit(policy, NAVIGATE, 1000);
        assertTrue(policy.submit(FOCUS, 1199));
        // This is later than the last accepted event, but earlier than the last observation.
        assertTrue(policy.submit(FOCUS, 1100));
        assertEquals(Optional.of(FOCUS), policy.drain(1100));
        assertTrue(policy.submit(CONFIRM, 1200));
        assertEquals(Optional.empty(), policy.drain(10));
        emit(policy, NAVIGATE, 10);
    }

    @Test void invalidCueDefinitionsAreRejectedBeforeTheyReachPlayback() {
        assertThrows(IllegalArgumentException.class, () -> cue(null, NAVIGATION, 200));
        assertThrows(IllegalArgumentException.class, () -> cue(" ", NAVIGATION, 200));
        assertThrows(NullPointerException.class, () -> cue("navigate", null, 200));
        assertThrows(IllegalArgumentException.class, () -> cue("navigate", NAVIGATION, -1));
        assertThrows(NullPointerException.class, () -> new UiSoundPolicy().submit(null, 0));
    }

    @Test void fastCarouselAndScrollEmitForEveryChangedStepTenMillisecondsApart() {
        var policy = new UiSoundPolicy();
        for (long now = 0; now <= 500; now += 10) emit(policy, now % 20 == 0 ? CAROUSEL : SCROLL, now);
    }

    @Test void ordinaryNavigationKeepsSharedTwoHundredMillisecondFloorAfterFastSelection() {
        var policy = new UiSoundPolicy();
        emit(policy, CAROUSEL, 1000);
        policy.submit(NAVIGATE, 1199);
        assertEquals(Optional.empty(), policy.drain(1199));
        emit(policy, NAVIGATE, 1200);
        // Restarting selection itself does not inherit the ordinary cue's navigation throttle.
        emit(policy, SCROLL, 1201);
    }

    @Test void resultPreemptsFastSelectionAndOwnsQuietGapWithoutQueuingIt() {
        var policy = new UiSoundPolicy();
        emit(policy, CAROUSEL, 1000);
        emit(policy, CONFIRM, 1001);
        policy.submit(SCROLL, 1050);
        assertEquals(Optional.empty(), policy.drain(1050));
        policy.submit(CAROUSEL, 1090);
        assertEquals(Optional.empty(), policy.drain(1090));
        assertEquals(Optional.empty(), policy.drain(1091));
        emit(policy, CAROUSEL, 1091);
        emit(policy, SCROLL, 1092);
    }

    @Test void structuralChangeWinsOverFastSelectionInEitherOrderAndKeepsQuietGap() {
        var policy = new UiSoundPolicy();
        policy.submit(CAROUSEL, 1000);
        policy.submit(OPEN, 1000);
        assertEquals(Optional.of(OPEN), policy.drain(1000));
        policy.submit(SCROLL, 1089);
        assertEquals(Optional.empty(), policy.drain(1089));
        emit(policy, SCROLL, 1090);
        policy.reset();
        policy.submit(OPEN, 1000);
        policy.submit(CAROUSEL, 1000);
        assertEquals(Optional.of(OPEN), policy.drain(1000));
    }

    @Test void fastSelectionCoalescesOncePerTickAndNeverReplaysOlderChanges() {
        var policy = new UiSoundPolicy();
        policy.submit(CAROUSEL, 1000);
        policy.submit(SCROLL, 1001);
        policy.submit(CAROUSEL, 1002);
        assertEquals(Optional.of(CAROUSEL), policy.drain(1020));
        assertEquals(Optional.empty(), policy.drain(1070));
        policy.submit(CAROUSEL, 1100);
        policy.submit(REJECT, 1100);
        assertEquals(Optional.of(REJECT), policy.drain(1100));
        assertEquals(Optional.empty(), policy.drain(1200));
    }

    @Test void resettingAfterResultClearsItsOwnershipAndPendingFastSelection() {
        var policy = new UiSoundPolicy();
        emit(policy, CONFIRM, 1000);
        policy.submit(CAROUSEL, 1001);
        policy.reset();
        assertEquals(Optional.empty(), policy.drain(1002));
        emit(policy, SCROLL, 1002);
        emit(policy, CAROUSEL, 1003);
    }

    private static UiSoundPolicy.Cue cue(String id, UiSoundPolicy.Priority priority, long repeatMillis) {
        return new UiSoundPolicy.Cue(id, priority, repeatMillis);
    }

    private static void emit(UiSoundPolicy policy, UiSoundPolicy.Cue cue, long now) {
        assertTrue(policy.submit(cue, now));
        assertEquals(Optional.of(cue), policy.drain(now));
    }
}
