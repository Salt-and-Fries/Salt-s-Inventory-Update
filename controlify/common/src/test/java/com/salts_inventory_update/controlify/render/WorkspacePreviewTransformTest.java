package com.salts_inventory_update.controlify.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WorkspacePreviewTransformTest {
    @Test void movesAndScalesNativePreviewAndModelTogether() {
        var mapped = WorkspacePreviewTransform.map(10, 20, 30, 60, 2, 0, 100, 0, 2, 50);
        assertEquals(new WorkspacePreviewTransform(120, 90, 160, 170, 2), mapped);
        assertEquals(70, mapped.modelScale(35));
    }

    @Test void fractionalAndRotatedBoundsRemainOutwardWithMatchingModelSize() {
        var fractional = WorkspacePreviewTransform.map(0, 0, 10, 20, .3, 0, .25, 0, .3, .75);
        assertEquals(new WorkspacePreviewTransform(0, 0, 4, 7, .3f), fractional);
        assertEquals(10.5f, fractional.modelScale(35));
        var rotated = WorkspacePreviewTransform.map(10, 20, 30, 60, 0, -2, 200, 2, 0, 50);
        assertEquals(new WorkspacePreviewTransform(80, 70, 160, 110, 2), rotated);
        assertEquals(70, rotated.modelScale(35));
    }

    @Test void identityRetainsOriginalPreviewAndModelSize() {
        var mapped = WorkspacePreviewTransform.map(10, 20, 30, 60, 1, 0, 0, 0, 1, 0);
        assertEquals(new WorkspacePreviewTransform(10, 20, 30, 60, 1), mapped);
        assertEquals(35, mapped.modelScale(35));
    }
}
