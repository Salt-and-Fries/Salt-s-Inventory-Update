package com.salts_inventory_update.compat.equipment;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.salts_inventory_update.compat.equipment.EquipmentPanelSelection.Kind.*;

class EquipmentPanelSelectionTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final class Panel {
        boolean open = true;
        Runnable onClose = () -> {};
        void close() { open = false; onClose.run(); }
    }
    private final EquipmentPanelSelection<Panel> views = new EquipmentPanelSelection<>(p -> p.open, Panel::close);

    @Test void switchingClosesTheOldPanelBeforeTheNewOpenRequest() {
        assertTrue(views.request(OWNER, CURIOS, 0));
        Panel curios = new Panel();
        assertTrue(views.accept(OWNER, CURIOS, curios));
        assertTrue(views.request(OWNER, ACCESSORIES, 1));
        assertFalse(curios.open);
        Panel accessories = new Panel();
        assertTrue(views.accept(OWNER, ACCESSORIES, accessories));
        assertTrue(views.request(OWNER, CURIOS, 2));
        assertFalse(accessories.open);
    }

    @Test void sameButtonClosesItsOpenPanel() {
        views.request(OWNER, ACCESSORIES, 0);
        Panel panel = new Panel(); views.accept(OWNER, ACCESSORIES, panel);
        assertFalse(views.request(OWNER, ACCESSORIES, 1));
        assertFalse(panel.open);
        assertFalse(views.accept(OWNER, ACCESSORIES, new Panel()));
    }

    @Test void lateResponseCannotReplaceTheLatestSelection() {
        views.request(OWNER, CURIOS, 0);
        assertTrue(views.request(OWNER, ACCESSORIES, 1));
        assertFalse(views.accept(OWNER, CURIOS, new Panel()));
        assertTrue(views.accept(OWNER, ACCESSORIES, new Panel()));
    }

    @Test void oldCloseCallbackCannotCancelTheNewRequest() {
        views.request(OWNER, CURIOS, 0);
        Panel old = new Panel(); views.accept(OWNER, CURIOS, old);
        old.onClose = () -> views.closed(OWNER, CURIOS, old);
        views.request(OWNER, ACCESSORIES, 1);
        assertTrue(views.accept(OWNER, ACCESSORIES, new Panel()));
    }

    @Test void duplicateResponseClosesOldHandleWithoutForgettingNewHandle() {
        views.request(OWNER, CURIOS, 0);
        Panel old = new Panel(); views.accept(OWNER, CURIOS, old);
        old.onClose = () -> views.closed(OWNER, CURIOS, old);
        Panel replacement = new Panel(); assertTrue(views.accept(OWNER, CURIOS, replacement));
        assertFalse(old.open);
        assertFalse(views.request(OWNER, CURIOS, 1));
        assertFalse(replacement.open);
    }

    @Test void pendingRequestsAreThrottledButCanRetryAfterTimeout() {
        assertTrue(views.request(OWNER, ACCESSORIES, 0));
        assertFalse(views.request(OWNER, ACCESSORIES, 1));
        assertTrue(views.request(OWNER, ACCESSORIES, 2_000_000_000L));
    }

    @Test void closingOtherKindLeavesCurrentSelectionAlone() {
        views.request(OWNER, ACCESSORIES, 0);
        views.close(OWNER, CURIOS);
        assertTrue(views.accept(OWNER, ACCESSORIES, new Panel()));
    }

    @Test void aetherOrParentCloseCancelsPendingGeneralView() {
        views.request(OWNER, CURIOS, 0);
        views.close(OWNER, CURIOS);
        assertFalse(views.accept(OWNER, CURIOS, new Panel()));
    }

    @Test void disconnectDiscardsInFlightRequests() {
        views.request(OWNER, ACCESSORIES, 0); views.clear();
        assertFalse(views.accept(OWNER, ACCESSORIES, new Panel()));
    }

    @Test void separateCharactersDoNotCloseEachOthersPanels() {
        UUID other = UUID.randomUUID();
        views.request(OWNER, CURIOS, 0);
        Panel first = new Panel(); views.accept(OWNER, CURIOS, first);
        views.request(other, ACCESSORIES, 1);
        assertTrue(views.accept(other, ACCESSORIES, new Panel()));
        assertTrue(first.open);
    }
}
