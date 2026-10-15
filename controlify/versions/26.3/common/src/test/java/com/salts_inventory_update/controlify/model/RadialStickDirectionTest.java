package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialStickDirectionTest {
    @Test void everyIndividualRecipeRemainsReachableWithIndependentAxisDeadzones() {
        int count = 1249;
        var selection = new ContextualWheelSelection();
        int sectors = selection.groupCount(count);
        for (int sector = 0; sector < sectors; sector++) {
            double angle = sector * Math.PI * 2 / sectors;
            float x = (float) Math.sin(angle), y = (float) -Math.cos(angle);
            var direction = RadialStickDirection.from(x, y, deadzone(x, 0.2f), deadzone(y, 0.2f));
            selection.update(count, direction.x(), direction.y(), 0, 0);
            assertEquals(sector, selection.selected(count), "recipe " + sector);
            assertEquals(Math.hypot(deadzone(x, 0.2f), deadzone(y, 0.2f)), Math.hypot(direction.x(), direction.y()), 0.00001);
        }
    }

    @Test void configuredDeadzonesRemainNeutralAndSubthresholdDeflectionDoesNotChangeSelection() {
        var neutral = RadialStickDirection.from(0.1f, 0.08f, 0, 0);
        assertEquals(new RadialStickDirection(0, 0), neutral);
        var selection = new ContextualWheelSelection();
        selection.update(40, 1, 0, 0, 0);
        int selected = selection.selected(40);
        var small = RadialStickDirection.from(-0.3f, 0.2f, -0.125f, 0);
        selection.update(40, small.x(), small.y(), neutral.x(), neutral.y());
        assertEquals(selected, selection.selected(40));
        assertEquals(new RadialStickDirection(0, 0), RadialStickDirection.from(Float.NaN, 0, 0, 0));
    }

    private static float deadzone(float value, float amount) {
        return Math.copySign(Math.max(0, Math.abs(value) - amount) / (1 - amount), value);
    }
}
