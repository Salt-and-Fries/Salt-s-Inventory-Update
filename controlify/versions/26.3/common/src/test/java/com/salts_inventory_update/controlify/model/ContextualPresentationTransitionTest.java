package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ContextualPresentationTransitionTest {
    @Test void openingAndClosingAreBoundedMonotonicAndFinishAtExactEndpoints() {
        var motion = new ContextualPresentationTransition();
        motion.target(true, 0, true);
        double previous = 0;
        for (long t = 0; t <= ContextualPresentationTransition.DURATION_NANOS; t += 10_000_000) {
            double next = motion.value(t);
            assertTrue(next >= previous && next <= 1); previous = next;
        }
        assertEquals(1, motion.value(ContextualPresentationTransition.DURATION_NANOS));
        motion.target(false, ContextualPresentationTransition.DURATION_NANOS, true);
        previous = 1;
        for (long t = ContextualPresentationTransition.DURATION_NANOS; t <= 2 * ContextualPresentationTransition.DURATION_NANOS; t += 10_000_000) {
            double next = motion.value(t);
            assertTrue(next <= previous && next >= 0); previous = next;
        }
        assertEquals(0, motion.value(2 * ContextualPresentationTransition.DURATION_NANOS));
        assertTrue(motion.finished(2 * ContextualPresentationTransition.DURATION_NANOS));
    }
    @Test void earlyReleaseAndRepressRetargetFromCurrentValueWithoutAJump() {
        var motion = new ContextualPresentationTransition();
        motion.target(true, 0, true);
        long release = 90_000_000;
        double before = motion.value(release);
        motion.target(false, release, true);
        assertEquals(before, motion.value(release));
        assertTrue(motion.value(release + 20_000_000) < before);
        long repress = release + 20_000_000;
        double closing = motion.value(repress);
        motion.target(true, repress, true);
        assertEquals(closing, motion.value(repress));
        assertTrue(motion.value(repress + 20_000_000) > closing);
    }
    @Test void repeatedHeldSamplesDoNotRestartTheAnimation() {
        var motion = new ContextualPresentationTransition();
        motion.target(true, 0, true);
        motion.target(true, 90_000_000, true);
        assertEquals(1, motion.value(ContextualPresentationTransition.DURATION_NANOS));
    }
    @Test void disabledAnimationsAndResetNeverLeaveARetainedIntermediatePose() {
        var motion = new ContextualPresentationTransition();
        motion.target(true, 0, true);
        motion.target(true, 90_000_000, false);
        assertEquals(1, motion.value(90_000_000));
        assertTrue(motion.finished(90_000_000));
        motion.target(false, 100_000_000, false);
        assertEquals(0, motion.value(100_000_000));
        motion.target(true, 110_000_000, true); motion.reset();
        assertEquals(0, motion.value(120_000_000));
    }
    @Test void changingSourcesResetsTheOldOpeningAndReverseBeforeStartingTheNewOrigin() {
        var motion = new ContextualPresentationTransition();
        motion.target(true, 0, true); motion.target(false, 90_000_000, true);
        assertTrue(motion.value(100_000_000) > 0);
        motion.reset(); motion.target(true, 100_000_000, true);
        var newSource = new ContextualPresentationLayout.Rect(430, 100, 176, 166);
        var newFrame = ContextualPresentationLayout.frame(760, 450, newSource, motion.value(100_000_000));
        assertEquals(newSource, newFrame.container());
        assertEquals(0, newFrame.progress());
        assertTrue(newFrame.wheel().y() > newSource.bottom());
    }
}
