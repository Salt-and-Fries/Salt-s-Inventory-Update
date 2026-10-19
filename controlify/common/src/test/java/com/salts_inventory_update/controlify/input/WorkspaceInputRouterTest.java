package com.salts_inventory_update.controlify.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceInputRouterTest {
    @Test
    void triggerSelectsOnReleaseAndSuppressesNativeClickOnReleaseTick() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        assertTrue(router.update(frame(true, false, false, false, false, 0)));
        assertTrue(actions.radial);
        assertTrue(router.update(frame(false, false, false, false, false, 10)));
        assertFalse(actions.radial);
        assertEquals(1, actions.committed);
        assertFalse(router.update(frame(false, false, false, false, false, 20)));
    }

    @Test
    void bumpersCycleOnlyTheSelectedSideAndSouthSwapsOnlyDuringRadial() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        assertFalse(router.update(frame(false, true, false, true, false, 0)));
        assertEquals(-1, actions.steps);
        assertEquals(0, actions.swapped);
        assertFalse(router.update(frame(false, false, true, false, false, 10)));
        assertEquals(0, actions.steps);
        assertTrue(router.update(frame(true, true, true, true, false, 20)));
        assertEquals(0, actions.steps);
        assertEquals(1, actions.swapped);
    }

    @Test
    void aShortClosePressClosesOneWindowOnlyInRadial() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        router.update(frame(false, false, false, false, true, 0));
        router.update(frame(false, false, false, false, false, 100));
        assertEquals(0, actions.closedWindow);
        router.update(frame(true, false, false, false, true, 200));
        router.update(frame(true, false, false, false, false, 300));
        assertEquals(1, actions.closedWindow);
        assertEquals(0, actions.closedAll);
    }

    @Test
    void simultaneousCloseAndTriggerReleaseClosesBeforeSelectionCommit() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        router.update(frame(true, false, false, false, true, 0));
        router.update(frame(false, false, false, false, false, 100));
        assertEquals("close,commit,", actions.order.toString());
    }

    @Test
    void holdMatchesCoreThresholdInBothModesAndFiresOnce() {
        for (boolean radial : new boolean[] {false, true}) {
            Actions actions = new Actions();
            actions.radial = radial;
            WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
            router.update(frame(radial, false, false, false, true, 20));
            router.update(frame(radial, false, false, false, true, 619));
            assertEquals(0, actions.closedAll);
            assertEquals(599 / 600f, router.closeProgress(619), 0.0001f);
            assertTrue(router.update(frame(radial, false, false, false, true, 620)));
            router.update(frame(radial, false, false, false, true, 1000));
            router.update(frame(radial, false, false, false, false, 1001));
            assertEquals(1, actions.closedAll);
            assertEquals(0, actions.closedWindow);
        }
    }

    @Test
    void tuckingAndInactivityClearPendingCloseWithoutClosingWindows() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        router.update(frame(false, false, false, false, true, 0));
        actions.tucked = true;
        assertTrue(router.update(frame(false, false, false, false, true, 500)));
        assertEquals(0f, router.closeProgress(500));
        actions.tucked = false;
        router.update(frame(false, false, false, false, true, 700));
        router.update(frame(false, false, false, false, true, 1299));
        assertEquals(0, actions.closedAll);
        actions.active = false;
        router.update(frame(false, false, false, false, true, 2000));
        assertEquals(0, actions.closedAll);
        assertEquals(0f, router.closeProgress(2000));
    }

    @Test
    void leftStickUsesScreenCoordinatesAndIsPassedOnlyDuringRadial() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        router.update(new ControllerInputFrame(false, 0.5f, -0.8f, false, false, false, false, 0));
        assertEquals(0, actions.directionUpdates);
        router.update(new ControllerInputFrame(true, 0.5f, -0.8f, false, false, false, false, 20));
        assertEquals(1, actions.directionUpdates);
        assertEquals(0.5f, actions.x);
        assertEquals(-0.8f, actions.y);
    }

    @Test
    void configuredCloseOwnsOverlappingSouthAndBumperBindings() {
        Actions actions = new Actions();
        WorkspaceInputRouter router = new WorkspaceInputRouter(actions);
        router.update(frame(true, false, false, true, true, 0));
        assertEquals(0, actions.swapped);
        router.update(frame(true, false, false, false, false, 100));
        assertEquals(1, actions.closedWindow);
        router.update(frame(false, false, false, false, false, 200));
        router.update(frame(false, true, true, false, true, 300));
        assertEquals(0, actions.steps);
    }

    private static ControllerInputFrame frame(boolean trigger, boolean left, boolean right,
                                               boolean south, boolean close, long time) {
        return new ControllerInputFrame(trigger, 0, 0, left, right, south, close, time);
    }

    private static final class Actions implements ControllerWorkspaceActions {
        boolean active = true;
        boolean radial;
        boolean tucked;
        int committed;
        int swapped;
        int steps;
        int closedWindow;
        int closedAll;
        int directionUpdates;
        float x;
        float y;
        StringBuilder order = new StringBuilder();

        @Override public boolean workspaceActive() { return active; }
        @Override public boolean radialActive() { return radial; }
        @Override public boolean tuckedAway() { return tucked; }
        @Override public void enterRadial() { radial = true; }
        @Override public void radialDirection(float x, float y) { this.x = x; this.y = y; directionUpdates++; }
        @Override public void releaseRadial() { radial = false; committed++; order.append("commit,"); }
        @Override public void cycleSelected(int steps) { this.steps += steps; }
        @Override public void swapFocused() { swapped++; }
        @Override public void closeHighlighted() { closedWindow++; order.append("close,"); }
        @Override public void closeWorkspace() { closedAll++; active = false; }
        @Override public void restoreWorkspace() { tucked = false; }
        @Override public long closeHoldDurationMillis() { return 600; }
    }
}
