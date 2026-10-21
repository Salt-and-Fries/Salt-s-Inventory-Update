package com.salts_inventory_update.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementControlStateTest {
    @Test
    void holdFollowsThePhysicalBinding() {
        var state = new MovementControlState();
        assertTrue(state.active(true, false, true));
        assertFalse(state.active(false, false, true));
        assertFalse(state.active(true, false, false));
    }

    @Test
    void quickTapLatchesUntilTheNextPress() {
        var state = new MovementControlState();
        state.press(true);
        assertTrue(state.active(false, true, true));
        assertTrue(state.active(false, true, true));
        state.press(true);
        assertFalse(state.active(true, true, true));
    }

    @Test
    void leavingTheWorkspaceOrChangingModeClearsToggle() {
        var state = new MovementControlState();
        state.press(true);
        assertFalse(state.active(true, true, false));
        assertFalse(state.active(false, true, true));
        state.press(true);
        assertFalse(state.active(false, false, true));
        assertFalse(state.active(false, true, true));
    }
}
