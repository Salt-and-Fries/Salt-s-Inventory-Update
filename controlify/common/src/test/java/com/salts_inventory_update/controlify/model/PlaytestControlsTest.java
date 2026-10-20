package com.salts_inventory_update.controlify.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaytestControlsTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary;

    @Test void triggerPreferencesDefaultToHoldAndPersistIndependently() throws Exception {
        var path = temporary.resolve("controller.properties");
        var config = com.salts_inventory_update.controlify.config.ControlifyConfig.load(path);
        assertFalse(config.toggleWindowWheel());
        assertFalse(config.toggleRecipeWheel());
        config.setToggleWindowWheel(true);
        config = com.salts_inventory_update.controlify.config.ControlifyConfig.load(path);
        assertTrue(config.toggleWindowWheel());
        assertFalse(config.toggleRecipeWheel());
        config.setToggleRecipeWheel(true);
        config.setToggleWindowWheel(false);
        config = com.salts_inventory_update.controlify.config.ControlifyConfig.load(path);
        assertFalse(config.toggleWindowWheel());
        assertTrue(config.toggleRecipeWheel());
        assertTrue(config.animationsEnabled());
    }
    @Test void togglePersistsThroughReleaseAndClosesOnTheNextPress() {
        var trigger = new TriggerToggle();
        assertTrue(trigger.update(true, true, true));
        assertTrue(trigger.update(true, true, true));
        assertTrue(trigger.update(false, true, true));
        assertFalse(trigger.update(true, true, true));
        assertFalse(trigger.update(false, true, true));
        assertTrue(trigger.update(true, true, true));
    }

    @Test void holdModeStillClosesOnReleaseAndBlockedUseCannotOpenAWheel() {
        var trigger = new TriggerToggle();
        assertFalse(trigger.update(true, false, false));
        assertFalse(trigger.update(true, false, true));
        assertFalse(trigger.update(false, false, true));
        assertTrue(trigger.update(true, false, true));
        assertFalse(trigger.update(false, false, true));
    }

    @Test void modalAndScreenChangesCancelLatchesUntilReleased() {
        var trigger = new TriggerToggle();
        assertTrue(trigger.update(true, true, true));
        trigger.cancel(true);
        assertFalse(trigger.update(true, true, true));
        assertFalse(trigger.update(false, true, true));
        assertTrue(trigger.update(true, true, true));
    }

    private record Recipe(String id, boolean craftable) { }
    @Test void craftingRefreshesCountsAndAvailabilityWithoutMovingOrRemovingRecipes() {
        var order = new StableRecipeOrder<Recipe>();
        var first = new Recipe("a", true);
        var second = new Recipe("b", true);
        assertEquals(List.of(first, second), order.update(List.of(first, second), List.of(first, second), Recipe::id));
        var depleted = new Recipe("a", false);
        var unlocked = new Recipe("aa", true);
        assertEquals(List.of(depleted, second), order.update(List.of(second, unlocked, depleted), List.of(second, unlocked), Recipe::id));
        order.reset();
        assertEquals(List.of(unlocked, second), order.update(List.of(depleted, unlocked, second), List.of(unlocked, second), Recipe::id));
    }

    @Test void paneToIconTransitionRemainsContinuousWhenRapidlyReversed() {
        var transition = new WorkspaceSurfaceTransition<String>();
        assertEquals(1, transition.value("chest", true, 0, true));
        assertEquals(1, transition.value("chest", false, 0, true));
        assertEquals(.5, transition.value("chest", false, 90_000_000, true), 1e-8);
        assertEquals(.5, transition.value("chest", true, 90_000_000, true), 1e-8);
        assertEquals(.75, transition.value("chest", true, 180_000_000, true), 1e-8);
        assertEquals(1, transition.value("chest", true, 270_000_000, true));
        assertEquals(0, transition.value("chest", false, 280_000_000, false));
    }

    @Test void swappingFocusKeepsTheOldCenterUnderTheHeldStickAndOtherIconsStayPut() {
        var workspace = new RadialWorkspace<>("player", "character");
        workspace.register("furnace", true);
        workspace.register("chest", true);
        workspace.register("table", true);
        workspace.open("chest");
        workspace.beginRadial();
        List<String> before = workspace.radialOrder();
        int chosen = before.indexOf("chest");
        double angle = RadialWorkspace.radialAngle(chosen, before.size());
        workspace.pointStick(Math.cos(angle), Math.sin(angle), .18);
        assertTrue(workspace.swapHighlighted());
        var expected = new java.util.ArrayList<>(before);
        expected.set(chosen, "player");
        assertEquals(expected, workspace.radialOrder());
        workspace.pointStick(Math.cos(angle), Math.sin(angle), .18);
        assertEquals("player", workspace.highlighted().orElseThrow());
        assertEquals("chest", workspace.focused());
        for (String id : workspace.windows()) workspace.register(id, true);
        assertEquals(expected, workspace.radialOrder(), "A Core window refresh must preserve the visual exchange");
        assertTrue(workspace.swapHighlighted());
        assertEquals(before, workspace.radialOrder());
        assertEquals(List.of("player", "character", "furnace", "chest", "table"), workspace.windows());
    }
}
