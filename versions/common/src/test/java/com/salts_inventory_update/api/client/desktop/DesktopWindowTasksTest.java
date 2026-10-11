package com.salts_inventory_update.api.client.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

final class DesktopWindowTasksTest {
    @Test void backgroundResultsAreAppliedOnlyByTheOwnerQueue() {
        var owner = new Owner();
        var scope = new DesktopWindowTasks(owner);
        var source = new CompletableFuture<Integer>();
        var value = new AtomicInteger();
        scope.watch(source, value::set, error -> fail(error));
        source.complete(42);
        assertEquals(0, value.get());
        owner.drain();
        assertEquals(42, value.get());
    }

    @Test void disposalSuppressesAlreadyQueuedResults() {
        var owner = new Owner();
        var scope = new DesktopWindowTasks(owner);
        var calls = new AtomicInteger();
        scope.watch(CompletableFuture.completedFuture(1), ignored -> calls.incrementAndGet(), error -> fail(error));
        scope.close();
        owner.drain();
        assertEquals(0, calls.get());
        assertTrue(scope.isClosed());
        scope.close();
    }

    @Test void cancellingASubviewObservationDoesNotCancelASharedFetch() throws Exception {
        var owner = new Owner();
        var scope = new DesktopWindowTasks(owner);
        var shared = new CompletableFuture<Integer>();
        var first = new AtomicInteger();
        var second = new AtomicInteger();
        var observation = scope.watch(shared, first::set, error -> fail(error));
        scope.watch(shared, second::set, error -> fail(error));
        observation.close();
        assertFalse(shared.isCancelled());
        shared.complete(7);
        owner.drain();
        assertEquals(0, first.get());
        assertEquals(7, second.get());
    }

    @Test void ownedWorkersCanObserveDisposalAndClosedScopesStartNoWork() {
        var owner = new Owner();
        var scope = new DesktopWindowTasks(owner);
        var cancellation = new AtomicReference<BooleanSupplier>();
        scope.submit(Runnable::run, token -> { cancellation.set(token); return 1; }, ignored -> { }, error -> fail(error));
        assertFalse(cancellation.get().getAsBoolean());
        scope.close();
        assertTrue(cancellation.get().getAsBoolean());
        var calls = new AtomicInteger();
        scope.submit(Runnable::run, token -> calls.incrementAndGet(), ignored -> { }, error -> fail(error));
        assertEquals(0, calls.get());
    }

    @Test void failuresReachTheOwnerWithoutCompletionWrapper() {
        var owner = new Owner();
        var scope = new DesktopWindowTasks(owner);
        var failure = new AtomicReference<Throwable>();
        var expected = new IllegalStateException("backend unavailable");
        scope.watch(CompletableFuture.failedFuture(new CompletionException(expected)), ignored -> fail(), failure::set);
        assertNull(failure.get());
        owner.drain();
        assertSame(expected, failure.get());
    }

    private static final class Owner implements DesktopWindowHandle {
        @Override public boolean cancelInput() { return isOpen(); }
        private final UUID id = UUID.randomUUID();
        private final List<Runnable> queued = new ArrayList<>();
        private boolean open = true;
        public UUID instanceId() { return id; }
        public boolean isOpen() { return open; }
        public boolean focus() { return open; }
        public boolean close() { boolean previous = open; open = false; return previous; }
        public DesktopWindowState state() { return new DesktopWindowState(id, open, 0, 0, 100, 100, false, false, false, false, false, false, open); }
        public boolean available(DesktopWindowAction action) { return open; }
        public boolean request(DesktopWindowAction action) { return open; }
        public boolean moveTo(int x, int y) { return open; }
        public boolean resizeTo(int width, int height) { return open; }
        public boolean schedule(Runnable action) { if (!open) return false; queued.add(action); return true; }
        void drain() { for (Runnable action : List.copyOf(queued)) if (open) action.run(); queued.clear(); }
    }
}
