package com.salts_inventory_update.controlify.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TextureSectorMaskTest {
    @Test void cardinalAimIncludesOnlyItsOwnSourceQuadrantAndRetainsPixelCoordinates() {
        assertTrue(covered(TextureSectorMask.create(288, 288, 0, TextureSectorMask.HALF_ANGLE), 144, 2));
        assertFalse(covered(TextureSectorMask.create(288, 288, 0, TextureSectorMask.HALF_ANGLE), 285, 144));
        var east = TextureSectorMask.create(288, 288, Math.PI / 2, TextureSectorMask.HALF_ANGLE);
        assertTrue(covered(east, 285, 144)); assertFalse(covered(east, 144, 2));
        // An asymmetric source texture keeps the east pixel's lighting, not a rotated north pixel.
        int[][] source = new int[288][288];
        for (int y = 0; y < 288; y++) for (int x = 0; x < 288; x++) source[y][x] = 0xFF000000 | x << 8 | y;
        int[][] output = new int[288][288];
        for (var strip : east) for (int y = strip.y(); y < strip.y() + strip.height(); y++)
            System.arraycopy(source[y], strip.x(), output[y], strip.x(), strip.width());
        assertEquals(source[144][285], output[144][285]);
        assertNotEquals(source[2][144], output[144][285]); assertEquals(0, output[2][144]);
    }
    @Test void everyCoveredPixelMatchesTheExactWedgeWithNoOverlapAndBoundedNativeRegions() {
        for (double angle : new double[]{0, .07, Math.PI / 4, Math.PI / 2, 2.3, Math.PI, 4.7, Math.PI * 2 - .01}) {
            var strips = TextureSectorMask.create(288, 288, angle, TextureSectorMask.HALF_ANGLE);
            assertFalse(strips.isEmpty()); assertTrue(strips.size() <= 288);
            boolean[][] pixels = new boolean[288][288]; int count = 0;
            for (var strip : strips) {
                assertTrue(strip.x() >= 0 && strip.y() >= 0 && strip.width() > 0 && strip.height() > 0);
                assertTrue(strip.x() + strip.width() <= 288 && strip.y() + strip.height() <= 288);
                for (int y = strip.y(); y < strip.y() + strip.height(); y++) for (int x = strip.x(); x < strip.x() + strip.width(); x++) {
                    assertFalse(pixels[y][x]); pixels[y][x] = true; count++;
                }
            }
            assertTrue(count < 288 * 288 / 10, "Only a small section of the full canvas is submitted");
            for (int y = 0; y < 288; y++) for (int x = 0; x < 288; x++) {
                double direction = Math.atan2(x + .5 - 144, -(y + .5 - 144));
                double difference = Math.atan2(Math.sin(direction - angle), Math.cos(direction - angle));
                assertEquals(Math.abs(difference) <= TextureSectorMask.HALF_ANGLE, pixels[y][x]);
            }
        }
    }
    @Test void northWraparoundIsSeamlessAndRepeatedAnglesAreDeterministic() {
        var angle = TextureSectorMask.create(288, 288, -.03, TextureSectorMask.HALF_ANGLE);
        assertEquals(angle, TextureSectorMask.create(288, 288, Math.PI * 2 - .03, TextureSectorMask.HALF_ANGLE));
        assertEquals(angle, TextureSectorMask.create(288, 288, -.03, TextureSectorMask.HALF_ANGLE));
        assertTrue(covered(angle, 140, 2)); assertTrue(covered(angle, 144, 2));
    }
    @Test void nonSquareSourceCanvasesAndInvalidRequestsAreHandledExplicitly() {
        var strips = TextureSectorMask.create(64, 32, 0, TextureSectorMask.HALF_ANGLE);
        assertTrue(covered(strips, 32, 0)); assertTrue(strips.size() <= 32);
        assertThrows(IllegalArgumentException.class, () -> TextureSectorMask.create(0, 32, 0, .1));
        assertThrows(IllegalArgumentException.class, () -> TextureSectorMask.create(32, 32, Double.NaN, .1));
        assertThrows(IllegalArgumentException.class, () -> TextureSectorMask.create(32, 32, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> TextureSectorMask.create(32, 32, 0, Math.PI + .01));
    }
    @Test void wideSectorsAndFullRingRetainEveryRequiredPixelWithoutBridgingTheGap() {
        for (double halfAngle : new double[]{Math.PI / 2, Math.PI * .8, Math.PI})
            for (double angle : new double[]{0, Math.PI / 2, 2.3, Math.PI * 2 - .01}) {
                var strips = TextureSectorMask.create(64, 64, angle, halfAngle);
                assertTrue(strips.size() <= 128);
                boolean[][] pixels = new boolean[64][64];
                for (var strip : strips) for (int y = strip.y(); y < strip.y() + strip.height(); y++)
                    for (int x = strip.x(); x < strip.x() + strip.width(); x++) {
                        assertFalse(pixels[y][x]); pixels[y][x] = true;
                    }
                for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                    double direction = Math.atan2(x + .5 - 32, -(y + .5 - 32));
                    double difference = Math.atan2(Math.sin(direction - angle), Math.cos(direction - angle));
                    assertEquals(Math.abs(difference) <= halfAngle, pixels[y][x]);
                }
            }
        assertEquals(List.of(new TextureSectorMask.Strip(0, 0, 64, 64)), TextureSectorMask.create(64, 64, 0, Math.PI));
    }
    private static boolean covered(List<TextureSectorMask.Strip> strips, int x, int y) {
        return strips.stream().anyMatch(s -> x >= s.x() && x < s.x() + s.width() && y >= s.y() && y < s.y() + s.height());
    }
}
