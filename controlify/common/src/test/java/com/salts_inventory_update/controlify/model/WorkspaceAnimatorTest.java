package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceAnimatorTest {
    private static WorkspaceLayout<String> frame(double x, double y, double scale, double highlight) {
        return new WorkspaceLayout<>(Map.of("player", new WindowPlacement(x, y, scale, 1, highlight)),
                new WindowPlacement(100, 200, 1, 0, 0));
    }

    @Test void disablingAnimationsMakesAllChangesImmediate() {
        var animator = new WorkspaceAnimator<String>();
        var start = frame(0, 0, 1, 0);
        assertEquals(start, animator.animate(start, 0, false, 200));
        var target = frame(100, -300, 0.5, 1);
        assertEquals(target, animator.animate(target, 20_000_000, false, 200));
        assertEquals(start, animator.animate(start, 30_000_000, true, 0));
    }

    @Test void enteringWorkspaceFadesAndMovesNewWindowsAndDoesNotRestartEveryFrame() {
        var animator = new WorkspaceAnimator<String>();
        var target = frame(100, 50, 1, 0);
        var start = animator.animate(target, 0, true, 200);
        assertEquals(0, start.windows().get("player").opacity());
        assertTrue(start.windows().get("player").y() < target.windows().get("player").y());
        var halfway = animator.animate(target, 100_000_000, true, 200);
        assertEquals(0.5, halfway.windows().get("player").opacity(), 1.0e-9);
        assertEquals(40, halfway.windows().get("player").y(), 1.0e-9);
        assertEquals(target, animator.animate(target, 200_000_000, true, 200));
        assertEquals(target, animator.animate(target, 500_000_000, true, 200));
    }

    @Test void retargetingASelectionOrFocusSwapContinuesFromCurrentPositionWithoutJumping() {
        var animator = new WorkspaceAnimator<String>();
        animator.animate(frame(0, 0, 1, 0), 0, false, 200);
        var target = frame(100, 200, 0.5, 1);
        animator.animate(target, 10_000_000, true, 200);
        var halfway = animator.animate(target, 110_000_000, true, 200);
        assertEquals(50, halfway.windows().get("player").x(), 1.0e-9);
        assertEquals(0.75, halfway.windows().get("player").scale(), 1.0e-9);
        assertEquals(0.5, halfway.windows().get("player").highlight(), 1.0e-9);
        var retarget = frame(-100, -100, 1.5, 0);
        assertEquals(halfway, animator.animate(retarget, 110_000_000, true, 200));
        var later = animator.animate(retarget, 210_000_000, true, 200);
        assertTrue(later.windows().get("player").x() < halfway.windows().get("player").x());
        assertEquals(retarget, animator.animate(retarget, 310_000_000, true, 200));
    }

    @Test void newAndClosingWindowsTransitionWithoutRecreatingSurvivors() {
        var animator = new WorkspaceAnimator<String>();
        var base = frame(10, 20, 1, 0);
        animator.animate(base, 0, false, 200);
        Map<String, WindowPlacement> windows = new LinkedHashMap<>(base.windows());
        var newWindow = new WindowPlacement(150, 30, 0.5, 1, 0);
        windows.put("chest", newWindow);
        var added = new WorkspaceLayout<>(windows, base.tuck());
        var start = animator.animate(added, 10_000_000, true, 200);
        assertEquals(base.windows().get("player"), start.windows().get("player"));
        assertEquals(0, start.windows().get("chest").opacity());
        assertEquals(added, animator.animate(added, 210_000_000, true, 200));
        var closing = animator.animate(base, 220_000_000, true, 200);
        assertEquals(1, closing.windows().get("chest").opacity());
        var halfway = animator.animate(base, 320_000_000, true, 200);
        assertEquals(0.5, halfway.windows().get("chest").opacity(), 1.0e-9);
        assertEquals(base, animator.animate(base, 420_000_000, true, 200));
        assertFalse(animator.animate(base, 500_000_000, true, 200).windows().containsKey("chest"));
    }

    @Test void tuckingAndRestoringMoveWorkspaceUpAndBackSmoothly() {
        var animator = new WorkspaceAnimator<String>();
        var shown = frame(40, 60, 0.6, 0);
        var tucked = frame(40, -500, 0.6, 0);
        animator.animate(shown, 0, false, 200);
        assertEquals(shown, animator.animate(tucked, 0, true, 200));
        var leaving = animator.animate(tucked, 100_000_000, true, 200);
        assertTrue(leaving.windows().get("player").y() < shown.windows().get("player").y());
        assertEquals(tucked, animator.animate(tucked, 200_000_000, true, 200));
        assertEquals(tucked, animator.animate(shown, 200_000_000, true, 200));
        var returning = animator.animate(shown, 300_000_000, true, 200);
        assertTrue(returning.windows().get("player").y() > tucked.windows().get("player").y());
        assertEquals(shown, animator.animate(shown, 400_000_000, true, 200));
    }
}
