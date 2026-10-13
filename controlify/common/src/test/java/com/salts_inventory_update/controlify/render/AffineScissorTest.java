package com.salts_inventory_update.controlify.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AffineScissorTest {
    @Test void translatedScaledNativeClipMatchesTransformedWindow() {
        assertEquals(new AffineScissor(120, 35, 60, 20),
                AffineScissor.transform(10, 20, 30, 40, 2, 0, 100, 0, 0.5, 25));
    }

    @Test void fractionalEdgesRoundOutwardWithoutClippingRenderedPixels() {
        assertEquals(new AffineScissor(0, 0, 2, 4),
                AffineScissor.transform(0, 0, 4, 8, 0.3, 0, 0.25, 0, 0.3, 0.75));
    }

    @Test void rotatedClipIncludesAllFourTransformedCorners() {
        assertEquals(new AffineScissor(40, 60, 40, 30),
                AffineScissor.transform(10, 20, 30, 40, 0, -1, 100, 1, 0, 50));
        // Opposite corners share X here; the other two define the clip width.
        assertEquals(new AffineScissor(-10, 0, 20, 20),
                AffineScissor.transform(0, 0, 10, 10, 1, -1, 0, 1, 1, 0));
    }

    @Test void reflectedClipKeepsOrderedBounds() {
        assertEquals(new AffineScissor(20, 20, 60, 40),
                AffineScissor.transform(10, 20, 30, 40, -2, 0, 100, 0, 1, 0));
    }

    @Test void originallyEmptyAndInvalidNativeClipsRemainEmpty() {
        assertEquals(0, AffineScissor.transform(10, 20, 0, 40, 2, 0, 100, 0, 2, 0).width());
        assertEquals(0, AffineScissor.transform(10, 20, 30, -1, 2, 0, 100, 0, 2, 0).height());
    }
}
