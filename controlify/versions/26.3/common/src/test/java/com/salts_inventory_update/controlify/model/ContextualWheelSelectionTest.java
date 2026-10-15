package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ContextualWheelSelectionTest {
    @Test void everyRecipeAndFirstLetterAnchorArrivesAtCarouselTop() {
        for (int count : new int[]{1, 9, 10, 11, 27, 643, 1249}) {
            var selection = new ContextualWheelSelection();
            for (int index = 0; index < count; index++) {
                point(selection, count, index, index * 1_000_000L);
                assertEquals(index, selection.selected(count));
                assertEquals(index, selection.visibleIndex(count, selection.topSlot(count)));
                var cards = selection.cards(count, index * 1_000_000L, false);
                final int recipeIndex = index;
                var top = cards.stream().filter(card -> card.recipeIndex() == recipeIndex).findFirst().orElseThrow();
                assertEquals(0, top.angle(), .00001);
                for (int slot = 0; slot < selection.visibleSize(count); slot++)
                    assertEquals(Math.floorMod(index - selection.topSlot(count) + slot, count), selection.visibleIndex(count, slot));
            }
        }
    }
    @Test void leftStickSelectsVisibleCardsWithoutChangingOuterPosition() {
        var selection = new ContextualWheelSelection(); point(selection, 40, 12.25, 0);
        assertEquals(12.25, selection.position(), .00001); selection.cards(40, 0, false);
        for (int slot = 0; slot < 10; slot++) {
            double angle = selection.displayedCardAngle(slot, 40);
            selection.update(40, 0, 0, (float)Math.sin(angle), (float)-Math.cos(angle), false, false, slot * 1_000_000L);
            assertEquals(7 + slot, selection.selected(40));
        }
        selection.update(40, .2f, .2f, .2f, .2f, false, false, 20_000_000);
        assertEquals(16, selection.selected(40)); assertEquals(12.25, selection.position(), .00001);
    }
    @Test void heldBumpersMoveContinuouslyAtFiveRecipesPerSecondAndFixedStickDoesNotResetThem() {
        var selection = new ContextualWheelSelection();
        double angle = marker(12, 40); float x = (float)Math.sin(angle), y = (float)-Math.cos(angle);
        selection.update(40, x, y, 0, 0, false, false, 0);
        selection.update(40, x, y, 0, 0, false, true, 100_000_000); assertEquals(12.5, selection.position(), .00001);
        selection.update(40, x, y, 0, 0, false, true, 200_000_000); assertEquals(13, selection.position(), .00001);
        selection.update(40, x, y, 0, 0, false, false, 300_000_000); assertEquals(13, selection.position(), .00001);
        selection.update(40, 0, 0, 0, 0, true, false, 400_000_000); assertEquals(12.5, selection.position(), .00001);
        selection.update(40, 0, 0, 0, 0, true, true, 500_000_000); assertEquals(12.5, selection.position(), .00001);
    }
    @Test void springReturnCannotChangeOuterOrInnerSelectionAndFreshDeflectionCan() {
        var selection = new ContextualWheelSelection(); point(selection, 100, 25, 0); selection.cards(100, 0, false);
        double innerAngle = selection.displayedCardAngle(7, 100);
        selection.update(100, 1, 0, (float)Math.sin(innerAngle), (float)-Math.cos(innerAngle), false, false, 10_000_000);
        assertEquals(27, selection.selected(100));
        selection.update(100, .81f, .12f, -.8f, .14f, false, false, 20_000_000);
        selection.update(100, .7f, -.12f, .65f, .15f, false, false, 30_000_000);
        selection.update(100, .5f, .15f, -.4f, .15f, false, false, 40_000_000);
        assertEquals(25, selection.position(), .00001); assertEquals(27, selection.selected(100));
        selection.update(100, 0, .72f, 0, -.72f, false, false, 50_000_000);
        assertEquals(25, selection.position(), .00001); assertEquals(27, selection.selected(100));
        point(selection, 100, 50, 60_000_000); assertEquals(50, selection.selected(100));
    }
    @Test void variableHighlightRepresentsVisibleCatalogRangeIncludingFullCircle() {
        var selection = new ContextualWheelSelection();
        assertEquals(Math.PI, selection.highlightHalfAngle(1)); assertEquals(Math.PI, selection.highlightHalfAngle(10));
        assertEquals(Math.PI / 2, selection.highlightHalfAngle(20)); assertEquals(Math.PI / 100, selection.highlightHalfAngle(1000));
        assertEquals(0, selection.highlightHalfAngle(0));
        assertTrue(selection.highlightHalfAngle(50) > selection.highlightHalfAngle(500));
    }
    @Test void reorderingPreservesSelectedIdentityAgainstUnchangedHeldStick() {
        var selection = new ContextualWheelSelection();
        double angle = marker(12, 40); float x = (float)Math.sin(angle), y = (float)-Math.cos(angle);
        selection.update(40, x, y, 0, 0, false, false, 0); selection.reanchor(20, 40);
        selection.update(40, x, y, 0, 0, false, false, 100_000_000);
        assertEquals(20, selection.selected(40)); assertEquals(20, selection.displayedSelected(40));
        selection.update(40, 0, 0, 0, 0, false, true, 500_000_000); assertEquals(22, selection.selected(40));
        assertThrows(IndexOutOfBoundsException.class, () -> selection.reanchor(40, 40));
    }
    @Test void carouselWrapsBothDirectionsWithoutPages() {
        var selection = new ContextualWheelSelection(); selection.update(11, 0, 0, 0, 0, false, false, 0);
        selection.update(11, 0, 0, 0, 0, true, false, 100_000_000);
        assertEquals(10.5, selection.position()); assertEquals(10, selection.selected(11)); assertEquals(5, selection.visibleIndex(11, 0));
        selection.update(11, 0, 0, 0, 0, false, true, 200_000_000);
        assertEquals(0, selection.position()); assertEquals(0, selection.selected(11));
    }
    @Test void cardsMoveFractionallyAndFadeThroughBottomOpening() {
        var selection = new ContextualWheelSelection(); selection.update(30, 0, 0, 0, 0, false, false, 0);
        var initial = selection.cards(30, 0, false); assertEquals(10, initial.size());
        var original = initial.stream().filter(card -> card.recipeIndex() == 25).findFirst().orElseThrow();
        selection.update(30, 0, 0, 0, 0, false, true, 100_000_000); var half = selection.cards(30, 100_000_000, false);
        var outgoing = half.stream().filter(card -> card.recipeIndex() == 25).findFirst().orElseThrow();
        var incoming = half.stream().filter(card -> card.recipeIndex() == 5).findFirst().orElseThrow();
        assertEquals(Math.toRadians(22.5), outgoing.angle() - original.angle(), .000001);
        assertEquals(.5, outgoing.opacity()); assertEquals(.5, incoming.opacity()); assertTrue(outgoing.scale() < original.scale());
        selection.update(30, 0, 0, 0, 0, false, true, 200_000_000);
        assertTrue(selection.cards(30, 200_000_000, false).stream().noneMatch(card -> card.recipeIndex() == 25));
    }
    @Test void largeMotionsAnimateAndOnlyDisplayedCardsAreActionable() {
        var selection = new ContextualWheelSelection(); selection.update(40, 0, 0, 0, 0, false, false, 0); selection.cards(40, 0, true);
        point(selection, 40, 17, 20_000_000); assertEquals(17, selection.selected(40)); assertEquals(0, selection.displayedSelected(40));
        for (long now = 40_000_000; now <= 200_000_000; now += 20_000_000) {
            var frame = selection.cards(40, now, true); int displayed = selection.displayedSelected(40);
            assertTrue(frame.stream().anyMatch(card -> card.recipeIndex() == displayed));
            assertTrue(frame.stream().allMatch(card -> card.opacity() > .001 && card.opacity() <= 1));
        }
        var settled = selection.cards(40, 250_000_000, false); assertEquals(17, selection.displayedSelected(40));
        assertEquals(0, settled.stream().filter(card -> card.recipeIndex() == 17).findFirst().orElseThrow().angle(), .00001);
    }
    @Test void outgoingCardsBelowFadeCutoffCannotBeSelected() {
        for (int count : new int[]{1, 11, 30}) {
            var selection = new ContextualWheelSelection(); point(selection, count, .9995, 0); var frame = selection.cards(count, 0, false);
            double outgoingAngle = selection.displayedCardAngle(0, count);
            selection.update(count, 0, 0, (float)Math.sin(outgoingAngle), (float)-Math.cos(outgoingAngle), false, false, 20_000_000);
            int selected = selection.displayedSelected(count);
            assertTrue(frame.stream().anyMatch(card -> card.recipeIndex() == selected && card.opacity() > .001));
            assertEquals(.9995, selection.position(), .00001);
        }
    }
    @Test void filterShrinkEmptyCatalogAndResetRemainValid() {
        var selection = new ContextualWheelSelection(); point(selection, 643, 640, 0);
        selection.update(3, 0, 0, 0, 0, false, false, 10_000_000); assertTrue(selection.selected(3) >= 0 && selection.selected(3) < 3);
        selection.update(0, 1, 1, 1, 1, true, true, 20_000_000);
        assertEquals(-1, selection.selected(0)); assertTrue(selection.cards(0, 20_000_000, true).isEmpty());
        selection.reset(); assertEquals(0, selection.selected(40));
    }
    @Test void anchoredCardArcKeepsBothVisibleEdgesAtAuthoredDoorwaySides() {
        var selection = new ContextualWheelSelection(); point(selection, 30, 0, 0);
        assertEquals(Math.toRadians(135), selection.cardAngle(0, 30), .000001);
        assertEquals(0, selection.cardAngle(selection.topSlot(30), 30), .000001);
        assertEquals(Math.toRadians(-135), selection.cardAngle(9, 30), .000001);
        point(selection, 30, .5, 100_000_000);
        assertEquals(Math.toRadians(157.5), selection.cardAngle(0, 30), .000001);
        assertEquals(Math.toRadians(-157.5), selection.cardAngle(10, 30), .000001);
    }
    @Test void bothCarouselEdgesCrossTheActualAuthoredOpeningAlpha() throws Exception {
        var folder = java.nio.file.Path.of("").toAbsolutePath();
        java.nio.file.Path texture = null;
        while (folder != null) {
            var candidate = folder.resolve("controlify/versions/26.3/common/src/main/resources/assets/salts_inventory_controlify/textures/gui/contextual/wheel/carousel_opening.png");
            if (java.nio.file.Files.isRegularFile(candidate)) { texture = candidate; break; }
            folder = folder.getParent();
        }
        assertNotNull(texture, "Authored doorway must remain available for alpha geometry verification");
        var image = javax.imageio.ImageIO.read(texture.toFile());
        var selection = new ContextualWheelSelection();
        point(selection, 30, .01, 0);
        assertTrue(maskedFraction(image, selection.cardAngle(10, 30), .01) > .9, "Incoming edge starts behind the authored doorway");
        point(selection, 30, .99, 10_000_000);
        assertTrue(maskedFraction(image, selection.cardAngle(0, 30), .01) > .9, "Outgoing edge reaches the authored doorway before wrapping");
        assertEquals(0, maskedFraction(image, 0, 1));
    }
    @Test void oneRecipeDoesNotDuplicateAndTwoRecipeIngressStartsAtHiddenBottomSeam() {
        var selection = new ContextualWheelSelection(); point(selection, 1, .4, 0);
        assertEquals(java.util.List.of(new ContextualWheelSelection.Card(0, 0, 1, 1)), selection.cards(1, 0, false));
        point(selection, 2, .01, 10_000_000);
        var incoming = selection.cards(2, 10_000_000, false).stream().filter(card -> card.opacity() < .1).findFirst().orElseThrow();
        assertTrue(Math.abs(incoming.angle()) > Math.toRadians(170));
        assertEquals(1, selection.visibleIndex(2, 0));
    }
    private static double maskedFraction(java.awt.image.BufferedImage image, double angle, double opacity) {
        int x = 380 + (int)Math.round(Math.sin(angle) * 108), y = 221 - (int)Math.round(Math.cos(angle) * 108);
        double half = 13 * (.78 + .22 * opacity); int total = 0, masked = 0;
        for (int py = (int)Math.ceil(y - half); py < Math.ceil(y + half); py++)
            for (int px = (int)Math.ceil(x - half); px < Math.ceil(x + half); px++) {
                total++;
                int u = px - 300, v = py - 280;
                if (u >= 0 && u < image.getWidth() && v >= 0 && v < image.getHeight() && (image.getRGB(u, v) >>> 24) > 0) masked++;
            }
        return masked / (double)total;
    }
    private static void point(ContextualWheelSelection selection, int count, double position, long now) {
        double angle = marker(position, count);
        selection.update(count, (float)Math.sin(angle), (float)-Math.cos(angle), 0, 0, false, false, now);
    }
    private static double marker(double position, int count) { return position * Math.PI * 2 / count; }
}
