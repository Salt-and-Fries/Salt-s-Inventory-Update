package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/** Per-window async scope. Results are marshalled to the owner; disposal suppresses late results. */
public final class DesktopWindowTasks implements AutoCloseable {
    private final DesktopWindowHandle owner;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CopyOnWriteArrayList<Runnable> cancellations = new CopyOnWriteArrayList<>();

    public DesktopWindowTasks(DesktopWindowHandle owner) { this.owner = Objects.requireNonNull(owner); }

    /** Observes a shared stage; closing this observation does not cancel someone else's stage. */
    public <T> AutoCloseable watch(CompletionStage<T> source, Consumer<T> success, Consumer<Throwable> failure) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(success);
        Objects.requireNonNull(failure);
        AtomicBoolean cancelled = new AtomicBoolean(closed.get());
        Runnable cancel = () -> cancelled.set(true);
        cancellations.add(cancel);
        if (closed.get()) cancel.run();
        source.whenComplete((value, error) -> {
            if (closed.get() || cancelled.get()) { cancellations.remove(cancel); return; }
            boolean scheduled = owner.schedule(() -> {
                try {
                    if (closed.get() || cancelled.get()) return;
                    if (error == null) success.accept(value);
                    else failure.accept(error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                } finally { cancellations.remove(cancel); }
            });
            if (!scheduled) cancellations.remove(cancel);
        });
        return () -> { cancel.run(); cancellations.remove(cancel); };
    }

    /** Owned background work receives a cooperative cancellation predicate. */
    public <T> AutoCloseable submit(Executor executor, Function<BooleanSupplier, T> work,
                                    Consumer<T> success, Consumer<Throwable> failure) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(work);
        Objects.requireNonNull(success);
        Objects.requireNonNull(failure);
        if (closed.get()) return () -> { };
        AtomicBoolean cancelled = new AtomicBoolean(closed.get());
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> work.apply(() -> closed.get() || cancelled.get()), executor);
        Runnable cancel = () -> { cancelled.set(true); future.cancel(true); };
        cancellations.add(cancel);
        if (closed.get()) cancel.run();
        AutoCloseable observation = watch(future, success, failure);
        future.whenComplete((value, error) -> cancellations.remove(cancel));
        return () -> { cancel.run(); cancellations.remove(cancel); observation.close(); };
    }

    public boolean isClosed() { return closed.get(); }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (Runnable cancel : cancellations) cancel.run();
        cancellations.clear();
    }
}
