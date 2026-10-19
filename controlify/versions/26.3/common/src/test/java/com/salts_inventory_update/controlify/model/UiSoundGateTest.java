package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class UiSoundGateTest {
    @Test void fastNavigationIsBoundedButNormalTwoPerSecondBumpersAreAudible() {
        var gate = new UiSoundGate();
        assertTrue(gate.allow("carousel", 110, 0));
        for (long now = 1; now < 110; now++) assertFalse(gate.allow("carousel", 110, now));
        assertTrue(gate.allow("carousel", 110, 110));
        assertTrue(gate.allow("carousel", 110, 500));
        assertTrue(gate.allow("carousel", 110, 1000));
    }
    @Test void OneInputCannotProduceASecondCueOfAnotherKind() {
        var gate = new UiSoundGate();
        assertTrue(gate.allow("category", 120, 1000));
        assertFalse(gate.allow("carousel", 110, 1000));
        assertFalse(gate.allow("confirm", 120, 1044));
        assertTrue(gate.allow("confirm", 120, 1045));
    }
    @Test void rejectedRepeatDoesNotPostponeNextUsefulCue() {
        var gate = new UiSoundGate();
        assertTrue(gate.allow("scroll", 100, 1000));
        assertFalse(gate.allow("scroll", 100, 1099));
        assertTrue(gate.allow("scroll", 100, 1100));
    }
    @Test void clockRestartRecoversAndNegativeIntervalsAreRejected() {
        var gate = new UiSoundGate();
        assertTrue(gate.allow("open", 160, 1000));
        assertTrue(gate.allow("open", 160, 10));
        assertThrows(IllegalArgumentException.class, () -> gate.allow("bad", -1, 11));
    }
}
