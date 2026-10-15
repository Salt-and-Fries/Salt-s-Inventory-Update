package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ContextualPresentationLayoutTest {
    private static final ContextualPresentationLayout.Rect SIDE = new ContextualPresentationLayout.Rect(35, 120, 250, 230);
    private static final ContextualPresentationLayout.Rect DESTINATION_SIDE = new ContextualPresentationLayout.Rect(430, 90, 260, 210);

    @Test void bothNativeWindowsStartAtTheirActualPairPositionsAndDestinationSettlesBelowMaterials() {
        var first = ContextualPresentationLayout.frame(760, 450, SIDE, DESTINATION_SIDE, 0, 176, 166, 194, 152);
        assertEquals(SIDE, first.container());
        assertEquals(DESTINATION_SIDE, first.destination());
        var settled = ContextualPresentationLayout.frame(760, 450, SIDE, DESTINATION_SIDE, 1, 176, 166, 194, 152);
        assertTrue(settled.container().x() >= ContextualPresentationLayout.SOURCE_AREA.x());
        assertTrue(settled.destination().y() >= settled.materials().bottom() + 14);
        assertEquals(settled.materials().centerX(), settled.destination().centerX(), 1e-9);
        assertEquals(194.0 / 152, settled.destination().width() / settled.destination().height(), 1e-9);
        assertTrue(settled.destination().bottom() <= settled.wheel().bottom());
        assertTrue(settled.destination().x() + settled.destination().width() < settled.wheel().x());
    }

    @Test void releaseAndRepressRetainBothActualPosesWhenTheSidePairGeometryChanges() {
        var displayed = ContextualPresentationLayout.frame(1280, 780, SIDE, DESTINATION_SIDE, .43, 176, 166, 194, 152);
        var changedDestination = new ContextualPresentationLayout.Rect(510, 130, 190, 155);
        var start = ContextualPresentationLayout.closing(1280, 700, SIDE, changedDestination, displayed, 1);
        assertEquals(displayed, start);
        var mid = ContextualPresentationLayout.closing(1280, 700, SIDE, changedDestination, displayed, .5);
        assertEquals((displayed.destination().x() + changedDestination.x()) / 2, mid.destination().x(), 1e-9);
        var end = ContextualPresentationLayout.closing(1280, 700, SIDE, changedDestination, displayed, 0);
        assertEquals(changedDestination, end.destination());
        assertEquals(mid, ContextualPresentationLayout.reopening(1280, 700, SIDE, changedDestination,
            176, 166, 194, 152, mid, 0));
    }

    @Test void tallDestinationAspectFitsTheLeftLowerAreaAtEveryViewportSize() {
        for (int[] viewport : new int[][]{{380,225}, {760,450}, {1280,720}, {1920,1080}}) {
            var frame = ContextualPresentationLayout.frame(viewport[0], viewport[1], SIDE, DESTINATION_SIDE, 1,
                176, 166, 195, 550);
            assertEquals(195.0 / 550, frame.destination().width() / frame.destination().height(), 1e-9);
            assertTrue(frame.destination().y() >= frame.materials().bottom());
            assertTrue(frame.destination().bottom() <= viewport[1]);
            assertTrue(frame.destination().x() >= 0);
        }
        assertThrows(IllegalArgumentException.class, () -> ContextualPresentationLayout.frame(760,450,SIDE,
            DESTINATION_SIDE,1,176,166,0,152));
    }

    @Test void firstFramePreservesRealSourceGeometryWhileBothPanelsEmergeBelowIt() {
        var frame = ContextualPresentationLayout.frame(760, 450, SIDE, 0);
        assertEquals(SIDE, frame.container());
        assertTrue(frame.materials().y() > SIDE.bottom());
        assertTrue(frame.wheel().y() > SIDE.bottom());
        assertEquals(SIDE.centerX(), frame.materials().centerX());
        assertEquals(SIDE.centerX(), frame.wheel().centerX());
        assertEquals(.15 * ContextualPresentationLayout.WHEEL.width(), frame.wheel().width(), 1e-9);
        assertEquals(0, frame.progress());
    }
    @Test void fullyOpenUsesExactFramedWheelAndMaterialsAreasAndAspectFitsCurrentNativeSource() {
        var frame = ContextualPresentationLayout.frame(760, 450, SIDE, 1, 176, 166);
        assertEquals(ContextualPresentationLayout.MATERIALS, frame.materials());
        assertEquals(ContextualPresentationLayout.WHEEL, frame.wheel());
        assertEquals(195, frame.container().width(), 1e-9);
        assertEquals(176.0 / 166, frame.container().width() / frame.container().height(), 1e-9);
        assertTrue(frame.container().x() >= 553 && frame.container().x() + frame.container().width() <= 748);
        assertTrue(frame.container().y() >= 87 && frame.container().bottom() <= 354);
    }
    @Test void nativeCreativeHeightChangesAffectTheTargetWithoutRewritingTheOpeningOrigin() {
        assertEquals(SIDE, ContextualPresentationLayout.frame(760, 450, SIDE, 0, 195, 390).container());
        var tall = ContextualPresentationLayout.frame(760, 450, SIDE, 1, 195, 390).container();
        assertEquals(267, tall.height(), 1e-9);
        assertEquals(.5, tall.width() / tall.height(), 1e-9);
    }
    @Test void everyVisualUsesTheSameProgressAndUniformPanelScaleAtAnIntermediateFrame() {
        var frame = ContextualPresentationLayout.frame(760, 450, SIDE, .4);
        assertEquals(.4, frame.progress());
        assertEquals(.49, frame.wheel().width() / ContextualPresentationLayout.WHEEL.width(), 1e-9);
        assertEquals(.49, frame.materials().width() / ContextualPresentationLayout.MATERIALS.width(), 1e-9);
        assertEquals(frame.wheel().width() / ContextualPresentationLayout.WHEEL.width(),
            frame.wheel().height() / ContextualPresentationLayout.WHEEL.height(), 1e-9);
        assertNotEquals(SIDE, frame.container());
    }
    @Test void earlyReleaseRetainsAllDisplayedGeometryDespiteChangedSideGuideReservationThenReturnsExactly() {
        var displayed = ContextualPresentationLayout.frame(1280, 780, SIDE, .35);
        var destination = new ContextualPresentationLayout.Rect(70, 85, 310, 285);
        var start = ContextualPresentationLayout.closing(1280, 730, destination, displayed, 1);
        assertEquals(displayed, start);
        var halfway = ContextualPresentationLayout.closing(1280, 730, destination, displayed, .5);
        assertEquals(.175, halfway.progress(), 1e-9);
        assertEquals((displayed.container().x() + destination.x()) / 2, halfway.container().x(), 1e-9);
        var end = ContextualPresentationLayout.closing(1280, 730, destination, displayed, 0);
        assertEquals(destination, end.container());
        assertEquals(0, end.progress());
    }
    @Test void reopeningDuringReverseBeginsFromVisibleGeometryAndEndsAtUpdatedNativeWindow() {
        var displayed = ContextualPresentationLayout.frame(760, 450, SIDE, .35);
        var closing = ContextualPresentationLayout.closing(760, 420, SIDE, displayed, .7);
        var start = ContextualPresentationLayout.reopening(760, 420, SIDE, 195, 390, closing, 0);
        assertEquals(closing, start);
        assertEquals(ContextualPresentationLayout.frame(760, 420, SIDE, 1, 195, 390),
            ContextualPresentationLayout.reopening(760, 420, SIDE, 195, 390, closing, 1));
    }
    @Test void contentViewportFitsSmallScreensAndRejectsInvalidDimensions() {
        var small = ContextualPresentationLayout.frame(380, 225, SIDE, 1);
        assertEquals(.5, small.canvasScale());
        assertEquals(0, small.canvasX()); assertEquals(0, small.canvasY());
        assertEquals(new ContextualPresentationLayout.Rect(104, 10.5, 172, 204), small.wheel());
        assertThrows(IllegalArgumentException.class, () -> ContextualPresentationLayout.frame(0, 450, SIDE, 1));
        assertThrows(IllegalArgumentException.class, () -> ContextualPresentationLayout.frame(760, 450, SIDE, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> ContextualPresentationLayout.frame(760, 450, SIDE, 1, 0, 166));
        assertThrows(IllegalArgumentException.class, () -> new ContextualPresentationLayout.Rect(0, 0, -1, 2));
    }
}
