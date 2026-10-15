package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ControllerScrollRepeatTest {
    @Test void driftDoesNotScrollAndAFrameBurstCannotSkipRows() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(0, repeat.update(0.54, 0));
        assertEquals(1, repeat.update(1, 10));
        for (long frame = 11; frame < 360; frame++) assertEquals(0, repeat.update(1, frame));
        assertEquals(1, repeat.update(1, 360));
        assertEquals(0, repeat.update(1, 519));
        assertEquals(1, repeat.update(1, 520));
    }

    @Test void releaseAndDirectionChangesAllowAFreshSingleStep() {
        var repeat = new ControllerScrollRepeat();
        assertEquals(1, repeat.update(0.7, 0));
        assertEquals(0, repeat.update(0, 10));
        assertEquals(1, repeat.update(0.7, 20));
        assertEquals(-1, repeat.update(-0.7, 30));
        assertEquals(-1, repeat.update(-0.7, 380));
        assertEquals(0, repeat.update(-0.7, 609));
        assertEquals(-1, repeat.update(-0.7, 610));
    }
}
