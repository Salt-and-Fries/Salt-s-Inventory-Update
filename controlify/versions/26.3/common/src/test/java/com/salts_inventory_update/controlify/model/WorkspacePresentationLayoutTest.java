package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkspacePresentationLayoutTest {
    private static final WindowSize STANDARD = new WindowSize(176, 166);
    private final WorkspacePresentationLayout layout = new WorkspacePresentationLayout();

    @Test void rapidHighlightChangesNeverResizeTheSettledCenter() {
        var workspace = workspace(8);
        var sizes = uniformSizes(workspace);
        sizes.put("player", new WindowSize(423, 108));
        sizes.put("window0", new WindowSize(500, 100));
        sizes.put("window1", new WindowSize(100, 400));
        workspace.beginRadial();
        var animator = new WorkspaceAnimator<String>();
        var initial = layout.compute(workspace, sizes, 854, 480, 1);
        animator.animate(initial, 0, false, 220);
        var center = initial.windows().get("player");
        long time = 0;
        for (int repeat = 0; repeat < 3; repeat++) {
            for (int i = 0; i < workspace.radialOrder().size(); i++) {
                double angle = RadialWorkspace.radialAngle(i, workspace.radialOrder().size());
                workspace.pointStick(Math.cos(angle), Math.sin(angle), .2);
                var target = layout.compute(workspace, sizes, 854, 480, 1);
                for (int ms = 0; ms <= 40; ms += 5) {
                    var frame = layout.constrainRadial(workspace, sizes,
                        animator.animate(target, time + ms * 1_000_000L, true, 220), 854, 480);
                    var sampled = frame.windows().get("player");
                    assertEquals(center.scale(), sampled.scale(), 1e-10, "Changing highlight must not resize center");
                    assertEquals(center.x(), sampled.x(), 1e-10);
                    assertEquals(center.y(), sampled.y(), 1e-10);
                }
                time += 45_000_000L;
            }
        }
    }

    @Test void centerStaysInsideOpaqueIconRingIncludingAnimatedFrames() {
        for (int count : List.of(1, 4, 12, 24)) {
            var workspace = workspace(count);
            var sizes = uniformSizes(workspace);
            sizes.put("player", new WindowSize(423, 108));
            var animator = new WorkspaceAnimator<String>();
            animator.animate(layout.compute(workspace, sizes, 854, 480, 1), 0, false, 220);
            workspace.beginRadial();
            var target = layout.compute(workspace, sizes, 854, 480, 1);
            for (int ms = 0; ms <= 240; ms += 10) {
                var frame = layout.constrainRadial(workspace, sizes,
                    animator.animate(target, ms * 1_000_000L, true, 220), 854, 480);
                var focus = frame.windows().get("player");
                assertTrue(Math.hypot(423 * focus.scale(), 108 * focus.scale()) < 2 * 480 * .39);
                for (String id : workspace.radialOrder()) {
                    var placement = frame.windows().get(id);
                    assertEquals(1, placement.opacity(), "Window icons remain opaque");
                    if (workspace.highlighted().filter(id::equals).isPresent()) continue; // Rendered as a live pane, not an icon.
                    var icon = WorkspacePreviewGeometry.icon(placement, sizes.get(id));
                    assertTrue(focus.x() + 423 * focus.scale() <= icon.x()
                        || focus.x() >= icon.x() + icon.size()
                        || focus.y() + 108 * focus.scale() <= icon.y()
                        || focus.y() >= icon.y() + icon.size(), "Center must not cover " + id + " at " + ms);
                }
            }
        }
    }

    @Test void sideShowsTwoSuccessiveGhostsPerDirectionWithDecreasingScaleAndOpacity() {
        var workspace = workspace(7);
        workspace.open("window2");
        var sizes = uniformSizes(workspace);
        var frame = layout.compute(workspace, sizes, 854, 480, 1);
        assertEquals(6, visible(frame).size(), "The focused pair and four neighboring previews are visible");
        var selected = frame.windows().get("window2");
        var above = frame.windows().get("window1");
        var secondAbove = frame.windows().get("window0");
        var below = frame.windows().get("window3");
        var secondBelow = frame.windows().get("window4");
        assertTrue(above.y() < selected.y());
        assertTrue(secondAbove.y() < above.y());
        assertTrue(below.y() > selected.y());
        assertTrue(secondBelow.y() > below.y());
        assertTrue(selected.scale() > above.scale() * 3);
        assertTrue(secondAbove.scale() < above.scale() * 0.6);
        assertTrue(secondBelow.scale() < below.scale() * 0.6);
        assertEquals(1, selected.opacity());
        assertTrue(above.opacity() < 0.4);
        assertTrue(secondAbove.opacity() < above.opacity());
        assertBoundedAndSeparate(frame, sizes, 854, 480);
    }

    @Test void sparseAndWrappedCyclesKeepNearestNeighborsAndNeverDuplicateContainers() {
        var mandatoryOnly = new RadialWorkspace<>("player", "character");
        mandatoryOnly.open("character");
        assertEquals(2, visible(layout.compute(mandatoryOnly, uniformSizes(mandatoryOnly), 320, 180, 1)).size());
        var workspace = workspace(2);
        workspace.open("window1");
        assertEquals(List.of("window0", "character"), WorkspacePresentationLayout.neighbors(workspace, -1));
        assertEquals(List.of("character", "window0"), WorkspacePresentationLayout.neighbors(workspace, 1));
        var sizes = uniformSizes(workspace);
        var frame = layout.compute(workspace, sizes, 320, 180, 1);
        assertEquals(4, visible(frame).size());
        assertTrue(frame.windows().get("window0").y() < frame.windows().get("window1").y());
        assertTrue(frame.windows().get("character").y() > frame.windows().get("window1").y());
        workspace.cycleSelected(1);
        assertEquals("character", workspace.selected());
        var wrapped = layout.compute(workspace, sizes, 320, 180, 1);
        assertTrue(wrapped.windows().get("window1").y() < wrapped.windows().get("character").y());
        assertTrue(wrapped.windows().get("window0").y() > wrapped.windows().get("character").y());
        assertBoundedAndSeparate(wrapped, sizes, 320, 180);
    }

    @Test void bumperAnimationBringsNeighborInAndPushesPreviousSelectionIntoOppositeGhost() {
        var workspace = workspace(7);
        workspace.open("window2");
        var sizes = uniformSizes(workspace);
        var old = layout.compute(workspace, sizes, 854, 480, 1);
        var animator = new WorkspaceAnimator<String>();
        animator.animate(old, 0, false, 220);
        workspace.cycleSelected(1);
        var next = layout.compute(workspace, sizes, 854, 480, 1);
        assertEquals(old, animator.animate(next, 10_000_000, true, 220));
        var half = animator.animate(next, 120_000_000, true, 220);
        var oldSelected = half.windows().get("window2");
        var incoming = half.windows().get("window3");
        assertTrue(oldSelected.y() < old.windows().get("window2").y());
        assertTrue(oldSelected.scale() < old.windows().get("window2").scale());
        assertTrue(oldSelected.opacity() < 1);
        assertTrue(incoming.y() < old.windows().get("window3").y());
        assertTrue(incoming.scale() > old.windows().get("window3").scale());
        assertTrue(incoming.opacity() > old.windows().get("window3").opacity());
        assertEquals(next, animator.animate(next, 230_000_000, true, 220));
        workspace.cycleSelected(-1);
        var reverse = layout.compute(workspace, sizes, 854, 480, 1);
        animator.animate(reverse, 240_000_000, true, 220);
        var returning = animator.animate(reverse, 350_000_000, true, 220);
        assertTrue(returning.windows().get("window3").y() > next.windows().get("window3").y());
        assertTrue(returning.windows().get("window2").scale() > next.windows().get("window2").scale());
    }

    @Test void radialFocusAndHighlightedChoiceStayLargeWhenManyThumbnailsShareTheWheel() {
        var workspace = workspace(24);
        var sizes = uniformSizes(workspace);
        workspace.beginRadial();
        var frame = layout.compute(workspace, sizes, 854, 480, 1);
        var focus = frame.windows().get("player");
        var highlighted = frame.windows().get(workspace.highlighted().orElseThrow());
        var ordinary = frame.windows().get("window5");
        assertTrue(focus.scale() >= 1);
        assertTrue(highlighted.scale() > ordinary.scale() * 4);
        assertTrue(focus.scale() > ordinary.scale() * 5);
        assertEquals(1, highlighted.opacity());
        assertEquals(1, ordinary.opacity());
        assertSeparate("focus", focus, STANDARD, "highlighted", highlighted, STANDARD);
    }

    @Test void everyViewportAndNativeShapeKeepsGhostsAndExpandedChoiceWithinScreenAndFocusClear() {
        for (double[] viewport : List.of(new double[]{1, 1}, new double[]{320, 180},
                new double[]{854, 480}, new double[]{1920, 1080}, new double[]{320, 1000})) {
            for (int count : List.of(1, 2, 3, 8, 24)) {
                var workspace = workspace(count);
                Map<String, WindowSize> sizes = new LinkedHashMap<>();
                sizes.put("player", new WindowSize(423, 108));
                sizes.put("character", new WindowSize(50, 300));
                for (int i = 0; i < count; i++) sizes.put("window" + i, new WindowSize(100 + i * 150, 50 + i * 73));
                var side = layout.compute(workspace, sizes, viewport[0], viewport[1], 3);
                assertBoundedAndSeparate(side, sizes, viewport[0], viewport[1]);
                workspace.beginRadial();
                for (int i = 0; i < workspace.radialOrder().size(); i++) {
                    double angle = RadialWorkspace.radialAngle(i, workspace.radialOrder().size());
                    workspace.pointStick(Math.cos(angle), Math.sin(angle), 0.2);
                    var frame = layout.compute(workspace, sizes, viewport[0], viewport[1], 3);
                    for (var entry : visible(frame)) assertBounded(entry.getValue(), sizes.get(entry.getKey()), viewport[0], viewport[1]);
                    String highlighted = workspace.highlighted().orElseThrow();
                    assertSeparate("player", frame.windows().get("player"), sizes.get("player"),
                            highlighted, frame.windows().get(highlighted), sizes.get(highlighted));
                    var placement = frame.windows().get(highlighted);
                    var size = sizes.get(highlighted);
                    double dx = placement.x() + size.width() * placement.scale() / 2 - viewport[0] / 2;
                    double dy = placement.y() + size.height() * placement.scale() / 2 - viewport[1] / 2;
                    workspace.pointStick(Math.cos(Math.atan2(dy, dx)), Math.sin(Math.atan2(dy, dx)), 0.2);
                    assertEquals(highlighted, workspace.highlighted().orElseThrow(), "Expansion keeps the original selection ray");
                }
            }
        }
    }

    @Test void tuckedAndInactiveWorkspacesRetainIdsAndHideTheirPresentation() {
        var workspace = workspace(7);
        var sizes = uniformSizes(workspace);
        workspace.beginRadial();
        workspace.pointStick(0, 1, 0.2);
        workspace.releaseRadial();
        var frame = layout.compute(workspace, sizes, 320, 180, 1);
        frame.windows().forEach((id, placement) -> assertTrue(placement.y() + sizes.get(id).height() * placement.scale() < 0));
        assertEquals(0, frame.tuck().opacity());
        workspace.restore();
        assertEquals(frame.windows().keySet(), layout.compute(workspace, sizes, 320, 180, 1).windows().keySet());
        workspace.closeAll();
        assertTrue(visible(layout.compute(workspace, sizes, 320, 180, 1)).isEmpty());
    }

    private static RadialWorkspace<String> workspace(int extra) {
        var workspace = new RadialWorkspace<>("player", "character");
        for (int i = 0; i < extra; i++) workspace.register("window" + i, true);
        workspace.open("window0");
        return workspace;
    }

    private static Map<String, WindowSize> uniformSizes(RadialWorkspace<String> workspace) {
        Map<String, WindowSize> sizes = new LinkedHashMap<>();
        workspace.windows().forEach(id -> sizes.put(id, STANDARD));
        return sizes;
    }

    private static List<Map.Entry<String, WindowPlacement>> visible(WorkspaceLayout<String> frame) {
        return frame.windows().entrySet().stream().filter(entry -> entry.getValue().opacity() > 0).toList();
    }

    private static void assertBoundedAndSeparate(WorkspaceLayout<String> frame, Map<String, WindowSize> sizes,
                                                double width, double height) {
        var visible = visible(frame);
        visible.forEach(entry -> assertBounded(entry.getValue(), sizes.get(entry.getKey()), width, height));
        for (int i = 0; i < visible.size(); i++) for (int j = i + 1; j < visible.size(); j++) {
            var a = visible.get(i);
            var b = visible.get(j);
            assertSeparate(a.getKey(), a.getValue(), sizes.get(a.getKey()), b.getKey(), b.getValue(), sizes.get(b.getKey()));
        }
    }

    private static void assertBounded(WindowPlacement placement, WindowSize size, double width, double height) {
        assertTrue(placement.x() >= -1.0e-7 && placement.y() >= -1.0e-7);
        assertTrue(placement.x() + size.width() * placement.scale() <= width + 1.0e-7);
        assertTrue(placement.y() + size.height() * placement.scale() <= height + 1.0e-7);
    }

    private static void assertSeparate(String a, WindowPlacement ap, WindowSize as, String b, WindowPlacement bp, WindowSize bs) {
        boolean apart = ap.x() + as.width() * ap.scale() <= bp.x() + 1.0e-7
                || bp.x() + bs.width() * bp.scale() <= ap.x() + 1.0e-7
                || ap.y() + as.height() * ap.scale() <= bp.y() + 1.0e-7
                || bp.y() + bs.height() * bp.scale() <= ap.y() + 1.0e-7;
        assertTrue(apart, () -> "Visible windows overlap: " + a + " and " + b);
    }
}
