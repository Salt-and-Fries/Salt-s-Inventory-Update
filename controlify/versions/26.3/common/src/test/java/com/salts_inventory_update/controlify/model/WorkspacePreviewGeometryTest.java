package com.salts_inventory_update.controlify.model;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspacePreviewGeometryTest {
    @Test void nativeItemSquareStaysCenteredInsideWideAndTallPreviewBounds() {
        for (var size : new WindowSize[]{new WindowSize(176, 64), new WindowSize(64, 500)}) {
            var placement = new WindowPlacement(12, 20, .2, .3, 0);
            var square = WorkspacePreviewGeometry.icon(placement, size);
            assertEquals(placement.x() + size.width() * placement.scale() / 2, square.x() + square.size() / 2);
            assertEquals(placement.y() + size.height() * placement.scale() / 2, square.y() + square.size() / 2);
            assertTrue(square.x() >= placement.x() && square.y() >= placement.y());
            assertTrue(square.x() + square.size() <= placement.x() + size.width() * placement.scale());
            assertTrue(square.y() + square.size() <= placement.y() + size.height() * placement.scale());
        }
    }

    @Test void tuckSelectionUsesSameRetargetableAnimationForGrowthAndShrink() {
        var workspace = new RadialWorkspace<>("inventory", "character");
        workspace.open("character"); workspace.beginRadial();
        var layout = new WorkspacePresentationLayout();
        var sizes = Map.of("inventory", new WindowSize(176, 166), "character", new WindowSize(176, 166));
        var animator = new WorkspaceAnimator<String>();
        var normal = layout.compute(workspace, sizes, 640, 400, 1);
        animator.animate(normal, 0, false, 220);
        workspace.pointStick(0, 1, .18);
        var selected = layout.compute(workspace, sizes, 640, 400, 1);
        animator.animate(selected, 10_000_000, true, 220);
        var growing = animator.animate(selected, 120_000_000, true, 220);
        assertTrue(growing.tuck().scale() > normal.tuck().scale());
        assertTrue(growing.tuck().scale() < selected.tuck().scale());
        animator.animate(selected, 230_000_000, true, 220);
        workspace.pointStick(-1, 0, .18);
        var unselected = layout.compute(workspace, sizes, 640, 400, 1);
        animator.animate(unselected, 240_000_000, true, 220);
        var shrinking = animator.animate(unselected, 350_000_000, true, 220);
        assertTrue(shrinking.tuck().scale() < selected.tuck().scale());
        assertTrue(shrinking.tuck().scale() > unselected.tuck().scale());
        assertEquals(32 * shrinking.tuck().scale(), WorkspacePreviewGeometry.tuck(shrinking.tuck()).size());
    }

    @Test void smallViewportKeepsBothTuckFramesInsideContentAndRetainsGrowthRatio() {
        var workspace = new RadialWorkspace<>("inventory", "character");
        workspace.open("character"); workspace.beginRadial();
        var layout = new WorkspacePresentationLayout();
        for (int height : new int[]{36, 80, 144, 180}) {
            workspace.pointStick(-1, 0, .18);
            var normal = layout.compute(workspace, Map.of(), 320, height, 1).tuck();
            workspace.pointStick(0, 1, .18);
            var selected = layout.compute(workspace, Map.of(), 320, height, 1).tuck();
            var square = WorkspacePreviewGeometry.tuck(selected);
            assertTrue(square.y() >= 0 && square.y() + square.size() <= height);
            assertEquals(1.15 / .68, selected.scale() / normal.scale(), 1e-10);
        }
    }
}
