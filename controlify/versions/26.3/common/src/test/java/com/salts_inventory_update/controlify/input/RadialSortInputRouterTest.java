package com.salts_inventory_update.controlify.input;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadialSortInputRouterTest {
    @Test
    void inventoryBindingSortsTheFocusedWindowOnlyWhileLeftTriggerIsHeld() {
        Actions actions = new Actions();
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        assertFalse(router.update(frame(false, false, 100), true, false));
        assertEquals(List.of(), actions.sorts);
        assertTrue(router.update(frame(true, false, 200), true, false));
        assertEquals(List.of("middle:false"), actions.sorts);
        actions.focused = "new middle";
        router.update(frame(true, false, 210), false, true);
        router.update(frame(true, false, 600), true, false);
        assertEquals(List.of("middle:false", "new middle:false"), actions.sorts);
    }

    @Test
    void quickSecondTapUsesCoresForceFlagEvenWhenTheFirstSortMovesNothing() {
        Actions actions = new Actions();
        actions.sortSucceeded = false;
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        router.update(frame(true, false, 1_000), true, false);
        router.update(frame(true, false, 1_050), false, true);
        router.update(frame(true, false, 1_000 + RadialSortInputRouter.DOUBLE_TAP_MILLIS), true, false);
        assertEquals(List.of("middle:false", "middle:true"), actions.sorts);
        router.update(frame(true, false, 1_360), false, true);
        router.update(frame(true, false, 1_400), true, false);
        assertEquals("middle:false", actions.sorts.get(2));
    }

    @Test
    void heldInventoryDoesNotRepeatAndOwnsItsReleaseAfterLeftTriggerIsReleased() {
        Actions actions = new Actions();
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        assertTrue(router.update(frame(true, false, 100), true, false));
        for (long time : new long[] {110, 800, 1_500}) {
            assertTrue(router.update(frame(true, false, time), true, true));
        }
        assertEquals(1, actions.sorts.size());
        assertTrue(router.update(frame(false, false, 1_600), true, true));
        assertTrue(router.update(frame(false, false, 1_650), true, true));
        assertTrue(router.update(frame(false, false, 1_700), false, true));
        assertFalse(router.update(frame(false, false, 1_750), false, false));
    }

    @Test
    void releasingLeftTriggerOrResettingStartsANewDoubleTapGesture() {
        Actions actions = new Actions();
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        router.update(frame(true, false, 100), true, false);
        router.update(frame(false, false, 120), false, true);
        router.update(frame(true, false, 140), true, false);
        router.reset();
        router.update(frame(true, false, 160), true, false);
        assertEquals(List.of("middle:false", "middle:false", "middle:false"), actions.sorts);
    }

    @Test
    void configuredBackBindingWinsWhenItOverlapsInventorySort() {
        Actions actions = new Actions();
        actions.radial = true;
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        router.update(frame(true, true, 100), true, false);
        router.update(frame(true, false, 150), false, true);
        assertEquals(List.of(), actions.sorts);
        assertEquals(1, actions.closedWindow);
        assertEquals(0, actions.closedAll);
    }

    @Test
    void radialBackClosesHighlightedImmediatelyAndHoldingNeverClosesAll() {
        for (long duration : new long[] {500, 1_250, 3_250, 10_000}) {
                Actions actions = new Actions();
                actions.radial = true;
                actions.duration = duration;
                RadialSortInputRouter router = new RadialSortInputRouter(actions);
                long start = 50_000;
                assertTrue(router.update(frame(true, true, start), false, false));
                assertEquals(1, actions.closedWindow);
                for (long elapsed : new long[] {duration - 1, duration, duration + 20_000}) {
                    assertTrue(router.update(frame(true, true, start + elapsed), false, false));
                    assertEquals(0, router.closeProgress(start + elapsed));
                    assertEquals(0, actions.closedAll);
                    assertEquals(1, actions.closedWindow);
                    assertTrue(actions.radial);
                }
                router.update(frame(true, false, start + duration + 20_100), false, false);
                assertEquals(1, actions.closedWindow);
                router.update(frame(true, true, start + duration + 20_200), false, false);
                assertEquals(2, actions.closedWindow);
        }
    }

    @Test
    void radialBackAndLeftTriggerReleaseCloseHighlightedBeforeLeavingRadial() {
        Actions actions = new Actions();
        actions.radial = true;
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        assertTrue(router.update(frame(false, true, 10_000), false, false));
        assertEquals(1, actions.closedWindow);
        assertEquals(0, actions.closedAll);
        assertFalse(actions.radial);
        assertEquals(0, router.closeProgress(10_600));
        assertTrue(router.update(frame(false, false, 10_600), false, false));
        assertFalse(router.update(frame(false, false, 10_700), false, false));
    }

    @Test
    void sideBackClosesImmediatelyAndOwnsHeldAndReleaseSamples() {
        Actions actions = new Actions();
        actions.duration = 10_000;
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        assertTrue(router.update(frame(false, true, 100), false, false));
        assertEquals(1, actions.closedAll);
        assertEquals(0, router.closeProgress(100));
        // A reopened UI cannot fire back again while the old press is still held.
        actions.active = true;
        router.workspaceOpened(false);
        assertTrue(router.update(frame(false, true, 120), false, false));
        assertTrue(router.update(frame(false, false, 140), false, false));
        assertEquals(1, actions.closedAll);
        assertFalse(router.update(frame(false, false, 160), false, false));
        router.update(frame(false, true, 180), false, false);
        assertEquals(2, actions.closedAll);
    }

    @Test
    void heldOpeningTriggerCannotEnterRadialUntilReleasedAndPressedAgain() {
        Actions actions = new Actions();
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        router.workspaceOpened(true);
        assertFalse(router.update(frame(true, false, 100), true, false));
        assertFalse(router.update(frame(true, false, 600), false, true));
        assertFalse(actions.radial);
        assertTrue(actions.sorts.isEmpty());
        assertFalse(router.update(frame(false, false, 620), false, false));
        assertTrue(router.update(frame(true, false, 640), false, false));
        assertTrue(actions.radial);
        router.update(frame(true, false, 660), true, false);
        assertEquals(List.of("middle:false"), actions.sorts);
    }

    @Test
    void contextualMaskCannotPretendThePhysicalOpeningTriggerWasReleased() {
        Actions actions = new Actions();
        RadialSortInputRouter router = new RadialSortInputRouter(actions);
        router.workspaceOpened(true);
        router.observeOpeningTrigger(true);
        router.update(frame(false, false, 100), false, false);
        assertTrue(router.awaitingOpeningTriggerRelease());
        assertFalse(actions.radial);
        router.observeOpeningTrigger(false);
        router.update(frame(false, false, 200), false, false);
        assertFalse(router.awaitingOpeningTriggerRelease());
        router.observeOpeningTrigger(true);
        router.update(frame(true, false, 300), false, false);
        assertTrue(actions.radial);
    }

    @Test
    void tuckedOrInactiveWorkspacesCannotStartSortsOrLeaveAnArmedDoubleTap() {
        for (boolean tucked : new boolean[] {false, true}) {
            Actions actions = new Actions();
            RadialSortInputRouter router = new RadialSortInputRouter(actions);
            router.update(frame(true, false, 100), true, false);
            actions.active = tucked;
            actions.tucked = tucked;
            router.update(frame(true, false, 120), false, true);
            router.update(frame(true, false, 140), true, false);
            assertEquals(List.of("middle:false"), actions.sorts);
            actions.active = true;
            actions.tucked = false;
            router.update(frame(true, false, 160), false, true);
            router.update(frame(true, false, 180), true, false);
            assertEquals(List.of("middle:false", "middle:false"), actions.sorts);
        }
    }

    private static ControllerInputFrame frame(boolean trigger, boolean close, long time) {
        return new ControllerInputFrame(trigger, 0, 0, false, false, false, close, time);
    }

    private static final class Actions implements RadialWorkspaceActions {
        boolean active = true;
        boolean radial;
        boolean tucked;
        boolean sortSucceeded = true;
        long duration = 500;
        int closedWindow;
        int closedAll;
        String focused = "middle";
        final List<String> sorts = new ArrayList<>();

        @Override public boolean workspaceActive() { return active; }
        @Override public boolean radialActive() { return radial; }
        @Override public boolean tuckedAway() { return tucked; }
        @Override public void enterRadial() { radial = true; }
        @Override public void radialDirection(float x, float y) {}
        @Override public void releaseRadial() { radial = false; }
        @Override public void cycleSelected(int steps) {}
        @Override public void swapFocused() {}
        @Override public void closeHighlighted() { closedWindow++; }
        @Override public void closeWorkspace() { closedAll++; active = false; }
        @Override public void restoreWorkspace() {}
        @Override public long closeHoldDurationMillis() { return duration; }
        @Override public boolean sortFocused(boolean force) {
            sorts.add(focused + ":" + force);
            return sortSucceeded;
        }
    }
}
