package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ControllerScrollRepeatTest {
    @Test void driftStaysStillAndHysteresisAllowsGentleScrollingWithoutChatter() {
        var repeat = new ControllerScrollRepeat();
        for (long frame = 0; frame <= 1000; frame += 50) {
            assertEquals(0, repeat.update(.4, frame));
            assertEquals(0, repeat.update(.54, frame));
        }
        assertEquals(1, repeat.update(.55, 1100));
        assertEquals(0, repeat.update(.5, 1200));
        assertEquals(0, repeat.update(.54, 1300));
        assertEquals(1, repeat.update(.5, 1350));
        assertEquals(0, repeat.update(.4, 1400));
        assertEquals(0, repeat.update(.5, 1500));
    }

    @Test void holdingFullStickHasNoInitialRepeatPauseAndCanAdvanceMultipleRows() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 0));
        assertEquals(1, repeat.update(1, 50));
        assertEquals(2, repeat.update(1, 150));
        assertEquals(3, repeat.update(1, 250));
    }

    @Test void steadyScrollingIsFrameRateIndependent() {
        for (long step : new long[] { 1, 10, 25, 50, 100 }) {
            assertEquals(25, scrollFor(1, 1000, step), "full stick at " + step + " ms per update");
            assertEquals(-25, scrollFor(-1, 1000, step), "reverse stick at " + step + " ms per update");
            assertEquals(5, scrollFor(.55, 1000, step), "gentle stick at " + step + " ms per update");
        }
    }

    @Test void strongerDeflectionSmoothlyIncreasesSpeed() {
        int gentle = scrollFor(.55, 2000, 10);
        int light = scrollFor(.65, 2000, 10);
        int medium = scrollFor(.75, 2000, 10);
        int strong = scrollFor(.9, 2000, 10);
        int full = scrollFor(1, 2000, 10);
        assertEquals(9, gentle);
        assertEquals(49, full);
        assertTrue(gentle < light && light < medium && medium < strong && strong < full);
        assertEquals(11, scrollFor(1, 450, 50));
    }

    @Test void releaseImmediatelyDiscardsFractionalMovementAndFreshEngagementStepsOnce() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 0));
        assertEquals(0, repeat.update(1, 40));
        assertEquals(0, repeat.update(0, 41));
        assertEquals(0, repeat.update(.4, 1000));
        assertEquals(1, repeat.update(1, 1010));
        assertEquals(0, repeat.update(1, 1011));
        repeat.reset();
        assertEquals(1, repeat.update(1, 1012));
        assertEquals(0, repeat.update(1, 1013));
    }

    @Test void reversingDirectionDiscardsOldFractionAndHonorsFreshActivationThreshold() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 0));
        assertEquals(0, repeat.update(1, 40));
        assertEquals(-1, repeat.update(-1, 41));
        assertEquals(0, repeat.update(-1, 42));
        assertEquals(0, repeat.update(.5, 43));
        assertEquals(0, repeat.update(.5, 1000));
        assertEquals(1, repeat.update(.55, 1001));
    }

    @Test void stalledFrameCatchupIsBoundedAndNeverQueuesOldRows() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 0));
        int resumed = repeat.update(1, 30_000);
        assertEquals(2, resumed);
        assertEquals(0, repeat.update(1, 30_000));
        assertEquals(0, repeat.update(1, 30_001));
        assertEquals(0, repeat.update(1, 30_010));
        assertEquals(1, repeat.update(1, 30_025));
    }

    @Test void clockRollbackRecoversAsFreshEngagement() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 1000));
        assertEquals(0, repeat.update(1, 1040));
        assertEquals(1, repeat.update(1, 20));
        assertEquals(0, repeat.update(1, 60));
        assertEquals(1, repeat.update(1, 62));
    }

    @Test void invalidInputStopsScrollingAndOvershootClampsToFullStickSpeed() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(1, 0));
        assertEquals(0, repeat.update(Double.NaN, 40));
        assertEquals(0, repeat.update(Double.POSITIVE_INFINITY, 50));
        assertEquals(0, repeat.update(Double.NEGATIVE_INFINITY, 60));
        assertEquals(0, repeat.update(.54, 70));
        assertEquals(25, scrollFor(2, 1000, 10));
    }

    private static int scrollFor(double vertical, long duration, long step) {
        var repeat = new ControllerScrollRepeat();
        int rows = repeat.update(vertical, 0);
        for (long time = step; time <= duration; time += step) rows += repeat.update(vertical, time);
        return rows;
    }
}
