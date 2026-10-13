package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.compat.ae2.client.DesktopContentRegistry;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DesktopPacketLifetimeTest {
    @Test void closedAndDisconnectedPendingPacketsAreDisposedExactlyOnce() {
        var disposed = new ArrayList<String>();
        var registry = new DesktopContentRegistry<String>(disposed::add);
        var closed = UUID.randomUUID();
        registry.awaitOpening(closed, "queued", 1);
        registry.remove(closed);
        registry.awaitOpening(closed, "late", 1);
        registry.awaitOpening(UUID.randomUUID(), "disconnected", 1);
        registry.clear(); registry.clear();
        assertEquals(List.of("queued", "late", "disconnected"), disposed);
    }

    @Test void overflowDisposesOnlyTheRejectedPacketUntilTheOwnerCloses() {
        var disposed = new ArrayList<Integer>();
        var registry = new DesktopContentRegistry<Integer>(disposed::add);
        var owner = UUID.randomUUID();
        for (int i = 0; i < 64; i++) registry.awaitOpening(owner, i, 1);
        assertThrows(IllegalStateException.class, () -> registry.awaitOpening(owner, 64, 1));
        assertEquals(List.of(64), disposed);
        registry.remove(owner);
        assertEquals(65, disposed.size());
        assertEquals(0, disposed.get(1));
        assertEquals(63, disposed.get(64));
    }

    @Test void aFailedReceiverDisposesTheUndeliveredTailAndPreservesOtherOwners() {
        var disposed = new ArrayList<String>();
        var registry = new DesktopContentRegistry<String>(disposed::add);
        var failed = UUID.randomUUID(); var other = UUID.randomUUID();
        registry.awaitOpening(failed, "first", 1);
        registry.awaitOpening(failed, "tail", 1);
        registry.awaitOpening(other, "other", 1);
        assertThrows(IllegalStateException.class, () -> registry.register(failed, packet -> {
            disposed.add(packet); throw new IllegalStateException("view failed");
        }));
        var delivered = new ArrayList<String>();
        registry.register(other, delivered::add);
        assertEquals(List.of("first", "tail"), disposed);
        assertEquals(List.of("other"), delivered);
    }

    @Test void earlyUpdatesReachTheirOwnWindowInWireOrder() {
        var registry = new DesktopContentRegistry<String>();
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        registry.awaitOpening(a, "a-first", 5);
        registry.awaitOpening(b, "b-first", 5);
        registry.awaitOpening(a, "a-second", 6);
        var seenA = new ArrayList<String>();
        var seenB = new ArrayList<String>();
        registry.register(b, seenB::add);
        registry.register(a, seenA::add);
        assertTrue(registry.receive(a, "a-third"));
        assertEquals(List.of("a-first", "a-second", "a-third"), seenA);
        assertEquals(List.of("b-first"), seenB);
    }

    @Test void closingOneWindowDropsItsLateUpdatesAndPreservesAnother() {
        var registry = new DesktopContentRegistry<String>();
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var seen = new ArrayList<String>();
        registry.register(a, seen::add);
        registry.register(b, seen::add);
        registry.remove(a);
        assertFalse(registry.receive(a, "stale"));
        registry.awaitOpening(a, "late", 4);
        registry.register(a, seen::add);
        assertTrue(registry.receive(b, "live"));
        assertEquals(List.of("live"), seen);
    }

    @Test void duplicateRegistrationCannotReplaceTheLiveReceiver() {
        var registry = new DesktopContentRegistry<String>();
        var owner = UUID.randomUUID();
        var seen = new ArrayList<String>();
        registry.register(owner, seen::add);
        assertThrows(IllegalStateException.class, () -> registry.register(owner, ignored -> fail()));
        assertTrue(registry.receive(owner, "original"));
        assertEquals(List.of("original"), seen);
    }

    @Test void queueLimitsRejectOverflowWithoutLosingAcceptedUpdates() {
        var registry = new DesktopContentRegistry<Integer>();
        var owner = UUID.randomUUID();
        for (int i = 0; i < 64; i++) registry.awaitOpening(owner, i, 1);
        assertThrows(IllegalStateException.class, () -> registry.awaitOpening(owner, 64, 1));
        var seen = new ArrayList<Integer>();
        registry.register(owner, seen::add);
        assertEquals(64, seen.size());
        assertEquals(0, seen.get(0));
        assertEquals(63, seen.get(63));
        var big = UUID.randomUUID();
        registry.awaitOpening(big, 1, 4 * 1024 * 1024);
        assertThrows(IllegalStateException.class, () -> registry.awaitOpening(big, 2, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> registry.awaitOpening(big, 3, -1));
    }

    @Test void openingFloodEvictsTheOldestPendingOwner() {
        var registry = new DesktopContentRegistry<Integer>();
        var oldest = UUID.randomUUID();
        registry.awaitOpening(oldest, 0, 1);
        var newest = oldest;
        for (int i = 1; i <= 16; i++) {
            newest = UUID.randomUUID();
            registry.awaitOpening(newest, i, 1);
        }
        var seen = new ArrayList<Integer>();
        registry.register(oldest, seen::add);
        assertTrue(seen.isEmpty());
        registry.register(newest, seen::add);
        assertEquals(List.of(16), seen);
    }

    @Test void disconnectDisposesReceiversAndQueuedUpdates() {
        var registry = new DesktopContentRegistry<String>();
        var live = UUID.randomUUID();
        var waiting = UUID.randomUUID();
        registry.register(live, ignored -> fail("old connection receiver"));
        registry.awaitOpening(waiting, "old connection packet", 21);
        registry.clear();
        assertFalse(registry.receive(live, "stale"));
        var seen = new ArrayList<String>();
        registry.register(waiting, seen::add);
        assertTrue(seen.isEmpty());
    }

    @Test void nestedMenuScopesRestoreTheOwnerEvenWhenTheCallbackFails() {
        var outer = new Object();
        var inner = new Object();
        assertNull(MenuPacketScope.current());
        MenuPacketScope.run(outer, () -> {
            assertSame(outer, MenuPacketScope.current());
            assertThrows(IllegalStateException.class, () -> MenuPacketScope.run(inner, () -> {
                assertSame(inner, MenuPacketScope.current());
                throw new IllegalStateException("widget failure");
            }));
            assertSame(outer, MenuPacketScope.current());
        });
        assertNull(MenuPacketScope.current());
    }

    @Test void nativeWidgetsUseNativeTransportUnlessAWindowOwnsTheScope() {
        var nativePackets = new ArrayList<String>();
        var scopedPackets = new ArrayList<String>();
        var owner = new Object();
        java.util.function.BiConsumer<Object, String> route = (menu, packet) -> {
            assertSame(owner, menu);
            scopedPackets.add(packet);
        };
        MenuPacketScope.send("native", route, nativePackets::add);
        MenuPacketScope.run(owner, () -> MenuPacketScope.send("desktop", route, nativePackets::add));
        MenuPacketScope.send("native-after", route, nativePackets::add);
        assertEquals(List.of("native", "native-after"), nativePackets);
        assertEquals(List.of("desktop"), scopedPackets);
    }
}
