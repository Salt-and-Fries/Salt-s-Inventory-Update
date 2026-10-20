package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RadialWorkspaceTest {
    private static RadialWorkspace<String> workspace() {
        return new RadialWorkspace<>("player", "character");
    }

    @Test void openingAnyUiShowsPlayerOnLeftAndOpenedUiOnRight() {
        var workspace = workspace();
        assertFalse(workspace.active());
        workspace.addAndOpen("chest", true);
        assertTrue(workspace.active());
        assertFalse(workspace.gameplayControls());
        assertEquals(RadialWorkspace.Mode.SIDE_BY_SIDE, workspace.mode());
        assertEquals("player", workspace.focused());
        assertEquals("chest", workspace.selected());
        assertEquals(List.of("player", "character", "chest"), workspace.windows());

        workspace.beginRadial();
        assertTrue(workspace.swapHighlighted());
        assertEquals("chest", workspace.focused());
        workspace.releaseRadial();
        workspace.addAndOpen("crafting", false);
        assertEquals("player", workspace.focused());
        assertEquals("crafting", workspace.selected());
        assertEquals(4, workspace.windows().size());
    }

    @Test void bumpersWrapClockwiseAndCounterclockwiseWithoutSelectingCenter() {
        var workspace = workspace();
        workspace.register("furnace", true);
        workspace.addAndOpen("chest", true);
        assertEquals(List.of("character", "furnace", "chest"), workspace.radialOrder());
        workspace.cycleSelected(1);
        assertEquals("character", workspace.selected());
        workspace.cycleSelected(-1);
        assertEquals("chest", workspace.selected());
        workspace.cycleSelected(-1);
        assertEquals("furnace", workspace.selected());
        assertEquals("player", workspace.focused());

        workspace.beginRadial();
        workspace.swapHighlighted();
        workspace.releaseRadial();
        assertEquals("furnace", workspace.focused());
        for (int i = 0; i < 8; i++) {
            assertNotEquals(workspace.focused(), workspace.selected());
            workspace.cycleSelected(1);
        }
    }

    @Test void radialStickSelectsNearestWindowAndTriggerReleaseCommitsIt() {
        var workspace = workspace();
        for (int i = 0; i < 7; i++) {
            workspace.register("window" + i, true);
        }
        workspace.open("window6");
        assertTrue(workspace.beginRadial());
        assertFalse(workspace.gameplayControls());
        var order = workspace.radialOrder();
        for (int i = 0; i < order.size(); i++) {
            double angle = RadialWorkspace.radialAngle(i, order.size());
            workspace.pointStick(Math.cos(angle), Math.sin(angle), 0.2);
            assertEquals(order.get(i), workspace.highlighted().orElseThrow());
            assertFalse(workspace.tuckHighlighted());
        }
        workspace.pointStick(0, 0, 0.2);
        assertEquals("window6", workspace.highlighted().orElseThrow(), "Deadzone preserves the highlight");
        assertTrue(workspace.releaseRadial());
        assertEquals("window6", workspace.selected());
        assertEquals("player", workspace.focused());
        assertEquals(RadialWorkspace.Mode.SIDE_BY_SIDE, workspace.mode());
    }

    @Test void aSwapsCenterAndLeavesPlayerAvailableInTheRing() {
        var workspace = workspace();
        workspace.addAndOpen("chest", true);
        workspace.beginRadial();
        assertTrue(workspace.swapHighlighted());
        assertEquals("chest", workspace.focused());
        assertEquals("player", workspace.highlighted().orElseThrow());
        assertEquals(List.of("character", "player"), workspace.radialOrder());
        assertTrue(workspace.releaseRadial());
        assertEquals("chest", workspace.focused());
        assertEquals("player", workspace.selected());
    }

    @Test void reservedBottomSectorTucksAndNewContainersPreserveExistingWorkspace() {
        var workspace = workspace();
        workspace.addAndOpen("chest", true);
        workspace.beginRadial();
        workspace.swapHighlighted();
        workspace.pointStick(0.2, 1, 0.2);
        assertTrue(workspace.tuckHighlighted());
        assertTrue(workspace.highlighted().isEmpty());
        assertFalse(workspace.swapHighlighted());
        assertTrue(workspace.closeHighlighted().isEmpty());
        workspace.releaseRadial();
        assertEquals(RadialWorkspace.Mode.TUCKED_AWAY, workspace.mode());
        assertTrue(workspace.gameplayControls());
        assertTrue(workspace.active());
        var previousWindows = workspace.windows();
        workspace.addAndOpen("furnace", true);
        assertEquals(RadialWorkspace.Mode.TUCKED_AWAY, workspace.mode());
        assertEquals("chest", workspace.focused());
        assertEquals("player", workspace.selected());
        assertEquals(previousWindows, workspace.windows().subList(0, previousWindows.size()));
        assertTrue(workspace.windows().contains("furnace"));
        assertFalse(workspace.beginRadial());
        assertEquals(List.of(), workspace.updateCloseHold(true, 10_000, 800));
        assertTrue(workspace.restore());
        assertFalse(workspace.gameplayControls());
        assertEquals("chest", workspace.focused());
        assertEquals("player", workspace.selected());
        assertEquals(4, workspace.windows().size());
    }

    @Test void bClosesOnlyClosableHighlightedWindowAndProtectsMandatoryWindows() {
        var workspace = workspace();
        workspace.register("character", true);
        workspace.register("player", true);
        workspace.register("nonclosable", false);
        workspace.addAndOpen("chest", true);
        workspace.beginRadial();
        assertEquals("chest", workspace.closeHighlighted().orElseThrow());
        assertFalse(workspace.windows().contains("chest"));
        assertEquals("character", workspace.selected());
        assertTrue(workspace.closeHighlighted().isEmpty());
        assertFalse(workspace.remove("player"));
        assertFalse(workspace.remove("character"));
        workspace.retain(List.of());
        assertEquals(List.of("player", "character"), workspace.windows());
        assertFalse(workspace.closable("player"));
        assertFalse(workspace.closable("character"));
    }

    @Test void holdingCloseUsesCoreThresholdInBothModesAndReleaseResetsProgress() {
        for (boolean radial : List.of(false, true)) {
            var workspace = workspace();
            workspace.register("nonclosable", false);
            workspace.register("furnace", true);
            workspace.addAndOpen("chest", true);
            if (radial) workspace.beginRadial();
            assertTrue(workspace.updateCloseHold(true, 400, 800).isEmpty());
            assertTrue(workspace.updateCloseHold(false, 100, 800).isEmpty());
            assertTrue(workspace.updateCloseHold(true, 799, 800).isEmpty());
            assertEquals(List.of("furnace", "chest"), workspace.updateCloseHold(true, 1, 800));
            assertFalse(workspace.active());
            assertTrue(workspace.gameplayControls());
            assertEquals(List.of("player", "character", "nonclosable"), workspace.windows());
            assertTrue(workspace.updateCloseHold(true, 5_000, 800).isEmpty());
        }
    }

    @Test void elapsedTimeCannotOverflowAndExternallyClosedFocusReturnsToPlayer() {
        var workspace = workspace();
        workspace.addAndOpen("chest", true);
        workspace.beginRadial();
        workspace.swapHighlighted();
        assertTrue(workspace.remove("chest"));
        assertEquals("player", workspace.focused());
        assertEquals("character", workspace.selected());
        assertNotEquals(workspace.focused(), workspace.highlighted().orElseThrow());
        workspace.register("furnace", true);
        assertEquals(List.of("furnace"), workspace.updateCloseHold(true, Long.MAX_VALUE, 800));
        assertFalse(workspace.active());
    }

    @Test void controllerDeactivationPreservesBackingWindowsAndCollectionViewsAreImmutable() {
        var workspace = workspace();
        workspace.addAndOpen("chest", true);
        assertThrows(UnsupportedOperationException.class, () -> workspace.windows().clear());
        assertThrows(UnsupportedOperationException.class, () -> workspace.radialOrder().clear());
        workspace.deactivate();
        assertFalse(workspace.active());
        assertTrue(workspace.gameplayControls());
        assertEquals(List.of("player", "character", "chest"), workspace.windows());
    }
}
