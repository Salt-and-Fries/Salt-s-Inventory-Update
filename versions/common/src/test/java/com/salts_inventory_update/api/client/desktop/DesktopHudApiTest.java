package com.salts_inventory_update.api.client.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class DesktopHudApiTest {
    private static final DesktopHudContext VIEWPORT = new DesktopHudContext(640, 450, 2);
    private static DesktopHudLayout base() { return DesktopHudLayout.standard(640, 450, 11); }

    @Test void noProviderRetainsVanillaGeometryAndExactHitEdges() {
        DesktopHudLayout layout = DesktopHudApi.resolve(VIEWPORT, base());
        assertEquals(10, layout.slots().size());
        var first = layout.slots().get(1);
        assertEquals(232, first.transform().x());
        assertEquals(431, first.transform().y());
        assertTrue(first.contains(231, 430));
        assertFalse(first.contains(249, 431));
        assertFalse(first.contains(Double.NaN, 431));
        assertEquals(203, layout.slots().get(0).transform().x());
    }

    @Test void liveOffsetsAndViewportChangesAreNeverCached() {
        AtomicInteger x = new AtomicInteger(0), y = new AtomicInteger(-2);
        try (var registration = DesktopHudApi.register("test:live", 0,
                (context, layout) -> layout.translated(x.get(), y.get()))) {
            assertEquals(429, DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1).transform().y());
            x.set(80); y.set(-60);
            var moved = DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1);
            assertTrue(moved.contains(313, 372));
            assertFalse(moved.contains(233, 432));
            var resized = DesktopHudApi.resolve(new DesktopHudContext(960, 600, 3),
                DesktopHudLayout.standard(960, 600, 11));
            assertEquals(472, resized.slots().get(1).transform().x());
        }
        assertEquals(base(), DesktopHudApi.resolve(VIEWPORT, base()));
    }

    @Test void independentSlotsCanFormScaledColumnsAndHideOrDisableTargets() {
        var column = base().mapSlots(slot -> slot.withTransform(new DesktopTransform(
            slot.kind() == DesktopHudSlot.Kind.OFFHAND ? 400 : 20, 30 + slot.index() * 40, 2)));
        var third = column.slots().get(3);
        assertTrue(third.contains(18, 108));
        assertFalse(third.contains(54, 110));
        assertFalse(third.withVisibility(false).contains(20, 110));
        assertFalse(third.withInteraction(false).contains(20, 110));
        var composed = third.transformed(new DesktopTransform(5, -10, 0.5));
        assertEquals(new DesktopTransform(15, 45, 1), composed.transform());
        assertTrue(composed.contains(15, 45));
    }

    @Test void orderingIsDeterministicAndLayoutsRemainImmutable() {
        List<String> calls = new ArrayList<>();
        try (var last = DesktopHudApi.register("test:last", 10, (context, layout) -> {
                calls.add("last"); return layout.translated(5, 0);
            });
            var z = DesktopHudApi.register("test:z", 0, (context, layout) -> { calls.add("z"); return layout; });
            var a = DesktopHudApi.register("test:a", 0, (context, layout) -> { calls.add("a"); return layout; })) {
            var original = base();
            assertEquals(237, DesktopHudApi.resolve(VIEWPORT, original).slots().get(1).transform().x());
            assertEquals(List.of("a", "z", "last"), calls);
            assertEquals(232, original.slots().get(1).transform().x());
            assertThrows(UnsupportedOperationException.class, () -> original.slots().clear());
            assertThrows(IllegalStateException.class, () -> DesktopHudApi.register("test:a", 0, (c, l) -> l));
        }
    }

    @Test void failedProviderRetainsPriorLayoutAllowsFollowingProviderAndRecovers() {
        AtomicInteger calls = new AtomicInteger();
        int before = DesktopDiagnostics.snapshot().size();
        try (var bad = DesktopHudApi.register("test:failing", 0, (context, layout) -> {
                if (calls.incrementAndGet() <= 2) throw new LinkageError("optional ABI mismatch");
                return layout.translated(10, 0);
            });
            var good = DesktopHudApi.register("test:remaining", 1, (context, layout) -> layout.translated(0, -2))) {
            assertEquals(232, DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1).transform().x());
            assertEquals(429, DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1).transform().y());
            assertEquals(before + 1, DesktopDiagnostics.snapshot().size(), "Repeated failures should not flood diagnostics");
            assertEquals(242, DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1).transform().x());
        }
    }

    @Test void failedIntegrationRollsBackOnlyItsOwnRegistrationAndStaleHandlesCannotRemoveReplacement() {
        assertThrows(IllegalStateException.class, () -> DesktopRegistrationScope.run("test:rollback", () -> {
            DesktopHudApi.register("test:rolled-back", 0, (context, layout) -> layout.translated(100, 0));
            throw new IllegalStateException("registration aborted");
        }));
        assertEquals(base(), DesktopHudApi.resolve(VIEWPORT, base()));
        var old = DesktopHudApi.register("test:replaced", 0, (context, layout) -> layout);
        old.close();
        try (var replacement = DesktopHudApi.register("test:replaced", 0, (context, layout) -> layout.translated(1, 0))) {
            old.close();
            assertEquals(233, DesktopHudApi.resolve(VIEWPORT, base()).slots().get(1).transform().x());
        }
    }

    @Test void invalidLayoutsAndNonRepresentableTransformsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopHudLayout(base().slots().subList(0, 9)));
        assertThrows(IllegalArgumentException.class, () -> base().mapSlots(slot -> base().slots().get(0)));
        assertThrows(IllegalArgumentException.class, () -> base().translated(Double.MAX_VALUE, 0));
        assertThrows(IllegalArgumentException.class, () -> base().transformed(new DesktopTransform(0, 0, Double.MIN_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> DesktopHudApi.register("unnamespaced", 0, (c, l) -> l));
        try (var invalid = DesktopHudApi.register("test:null", 0, (c, l) -> null)) {
            assertEquals(base(), DesktopHudApi.resolve(VIEWPORT, base()));
        }
    }
}
