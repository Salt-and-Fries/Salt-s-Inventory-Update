package com.salts_inventory_update.controlify.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RadialLayoutTest {
    private static final double EPS = 1.0e-7;
    private final RadialLayout layout = new RadialLayout();

    @Test void onlyTwoLargeWindowsAreVisibleWithFocusOnLeftAndSelectionOnRight() {
        var workspace = new RadialWorkspace<>("player", "character");
        workspace.register("furnace", true);
        workspace.addAndOpen("chest", true);
        var size = new WindowSize(176, 166);
        var sizes = Map.of("player", size, "character", size, "chest", size, "furnace", size);
        var frame = layout.compute(workspace, sizes, 854, 480, 1);
        assertEquals(2, frame.windows().values().stream().filter(p -> p.opacity() > 0).count());
        var left = frame.windows().get("player");
        var right = frame.windows().get("chest");
        assertTrue(left.x() + size.width() * left.scale() < right.x());
        assertTrue(left.scale() > 1, "Side-by-side windows should use available screen space");
        assertEquals(0, frame.tuck().opacity());
        workspace.beginRadial();
        var radial = layout.compute(workspace, sizes, 854, 480, 1);
        assertTrue(radial.windows().get("player").scale() < left.scale());
        assertEquals(4, radial.windows().values().stream().filter(p -> p.opacity() > 0).count());
        assertEquals(1, radial.tuck().opacity());
    }

    @Test void heterogeneousWindowsRemainBoundedAndSeparateAcrossViewportSizesAndCounts() {
        for (double[] viewport : List.of(new double[]{1, 1}, new double[]{320, 180},
                new double[]{854, 480}, new double[]{1920, 1080}, new double[]{320, 1000})) {
            for (int count : List.of(1, 2, 3, 8, 24)) {
                var workspace = new RadialWorkspace<>("player", "character");
                Map<String, WindowSize> sizes = new HashMap<>();
                sizes.put("player", new WindowSize(176, 166));
                sizes.put("character", new WindowSize(50, 300));
                for (int i = 0; i < count; i++) {
                    String id = "window" + i;
                    workspace.register(id, true);
                    sizes.put(id, new WindowSize(100 + i * 150, 50 + i * 73));
                }
                workspace.open("window0");
                for (boolean radial : List.of(false, true)) {
                    if (radial) workspace.beginRadial();
                    var frame = layout.compute(workspace, sizes, viewport[0], viewport[1], 3);
                    assertBoundedAndSeparate(frame, sizes, viewport[0], viewport[1]);
                    if (radial) {
                        for (int i = 0; i < workspace.radialOrder().size(); i++) {
                            double angle = RadialWorkspace.radialAngle(i, workspace.radialOrder().size());
                            workspace.pointStick(Math.cos(angle), Math.sin(angle), 0.2);
                            assertBoundedAndSeparate(layout.compute(workspace, sizes, viewport[0], viewport[1], 3),
                                    sizes, viewport[0], viewport[1]);
                        }
                    }
                }
            }
        }
    }

    @Test void stickDirectionMatchesRenderedWindowCentersAndBottomSectorIsReserved() {
        var workspace = new RadialWorkspace<>("player", "character");
        Map<String, WindowSize> sizes = new HashMap<>();
        sizes.put("player", new WindowSize(176, 166));
        sizes.put("character", new WindowSize(80, 100));
        for (int i = 0; i < 10; i++) {
            workspace.register("window" + i, true);
            sizes.put("window" + i, new WindowSize(170 + i * 10, 70));
        }
        workspace.open("window0");
        workspace.beginRadial();
        var frame = layout.compute(workspace, sizes, 854, 480, 1);
        for (var id : workspace.radialOrder()) {
            var placement = frame.windows().get(id);
            var size = sizes.get(id);
            double dx = placement.x() + size.width() * placement.scale() / 2 - 427;
            double dy = placement.y() + size.height() * placement.scale() / 2 - 240;
            workspace.pointStick(dx / 100, dy / 100, 0.2);
            assertEquals(id, workspace.highlighted().orElseThrow());
            assertFalse(workspace.tuckHighlighted());
        }
        workspace.pointStick(0, 1, 0.2);
        assertTrue(workspace.tuckHighlighted());
        assertEquals(1, layout.compute(workspace, sizes, 854, 480, 1).tuck().highlight());
    }

    @Test void tuckingMovesEveryExistingWindowFullyAboveViewportAndRestoreKeepsIds() {
        var workspace = new RadialWorkspace<>("player", "character");
        workspace.addAndOpen("chest", true);
        var size = new WindowSize(176, 166);
        var sizes = Map.of("player", size, "character", size, "chest", size);
        workspace.beginRadial();
        workspace.pointStick(0, 1, 0.2);
        workspace.releaseRadial();
        var tucked = layout.compute(workspace, sizes, 320, 180, 1);
        assertEquals(workspace.windows().size(), tucked.windows().size());
        tucked.windows().forEach((id, placement) ->
                assertTrue(placement.y() + sizes.get(id).height() * placement.scale() < 0));
        workspace.restore();
        var restored = layout.compute(workspace, sizes, 320, 180, 1);
        assertEquals(tucked.windows().keySet(), restored.windows().keySet());
        assertBoundedAndSeparate(restored, sizes, 320, 180);
        workspace.closeAll();
        assertTrue(layout.compute(workspace, sizes, 320, 180, 1).windows().values()
                .stream().allMatch(placement -> placement.opacity() == 0));
    }

    private static void assertBoundedAndSeparate(WorkspaceLayout<String> frame, Map<String, WindowSize> sizes,
                                                double width, double height) {
        var visible = frame.windows().entrySet().stream().filter(entry -> entry.getValue().opacity() > 0).toList();
        for (var entry : visible) {
            var placement = entry.getValue();
            var size = sizes.get(entry.getKey());
            assertTrue(placement.x() >= -EPS && placement.y() >= -EPS, "Top-left must be within viewport");
            assertTrue(placement.x() + size.width() * placement.scale() <= width + EPS);
            assertTrue(placement.y() + size.height() * placement.scale() <= height + EPS);
        }
        for (int i = 0; i < visible.size(); i++) {
            for (int j = i + 1; j < visible.size(); j++) {
                var a = visible.get(i);
                var b = visible.get(j);
                var ap = a.getValue();
                var bp = b.getValue();
                var as = sizes.get(a.getKey());
                var bs = sizes.get(b.getKey());
                boolean apart = ap.x() + as.width() * ap.scale() <= bp.x() + EPS
                        || bp.x() + bs.width() * bp.scale() <= ap.x() + EPS
                        || ap.y() + as.height() * ap.scale() <= bp.y() + EPS
                        || bp.y() + bs.height() * bp.scale() <= ap.y() + EPS;
                assertTrue(apart, () -> "Visible windows overlap: " + a.getKey() + " and " + b.getKey());
            }
        }
    }
}
