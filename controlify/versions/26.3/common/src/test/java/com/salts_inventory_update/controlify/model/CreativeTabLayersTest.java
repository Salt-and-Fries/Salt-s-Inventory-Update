package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CreativeTabLayersTest {
    @Test void opaqueHoverDoesNotEraseActualSelectionWhileNativeItemCenterRemainsUncovered() {
        int[] marker = {0xFF858585, 0xFFD4AF36, 0xFF8DE7BB};
        int[] center = {0xFF858585, 0xFFD4AF36, 0};
        int selectedMarker = 0, selectedCenter = 0;
        for (var layer : CreativeTabLayers.ordered(true, true)) {
            if ((marker[layer.ordinal()] >>> 24) > 0) selectedMarker = marker[layer.ordinal()];
            if ((center[layer.ordinal()] >>> 24) > 0) selectedCenter = center[layer.ordinal()];
        }
        assertEquals(0xFF8DE7BB, selectedMarker);
        assertEquals(0xFFD4AF36, selectedCenter);
    }
    @Test void movingHoverToAnotherTabNeverFabricatesOrRemovesTheNativeSelection() {
        assertTrue(CreativeTabLayers.ordered(false, true).contains(CreativeTabLayers.Layer.ACTIVE));
        assertFalse(CreativeTabLayers.ordered(true, false).contains(CreativeTabLayers.Layer.ACTIVE));
        assertFalse(CreativeTabLayers.ordered(false, false).contains(CreativeTabLayers.Layer.HOVER));
    }
}
