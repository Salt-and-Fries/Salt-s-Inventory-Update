package com.salts_inventory_update.api.integration;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Bounded, structured integration diagnostics. Listener callbacks run on the reporting thread. */
public final class DesktopDiagnostics {
    private static final int LIMIT = 256;
    private static final ArrayDeque<Event> EVENTS = new ArrayDeque<>();
    private static final CopyOnWriteArrayList<Consumer<Event>> LISTENERS = new CopyOnWriteArrayList<>();

    private DesktopDiagnostics() { }

    public enum Level { INFO, WARNING, ERROR }

    public record Event(Level level, String owner, String operation, String message, Throwable cause) { }

    public static void report(Level level, String owner, String operation, String message, Throwable cause) {
        Event event = new Event(Objects.requireNonNull(level), Objects.requireNonNull(owner),
            Objects.requireNonNull(operation), Objects.requireNonNull(message), cause);
        synchronized (EVENTS) {
            if (EVENTS.size() == LIMIT) EVENTS.removeFirst();
            EVENTS.addLast(event);
        }
        for (Consumer<Event> listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // A diagnostics observer must not break registration or an inventory operation.
            }
        }
    }

    public static List<Event> snapshot() {
        synchronized (EVENTS) {
            return List.copyOf(EVENTS);
        }
    }

    /** Close the subscription to remove the listener, for example when a debug panel closes. */
    public static AutoCloseable listen(Consumer<Event> listener) {
        LISTENERS.add(Objects.requireNonNull(listener));
        return () -> LISTENERS.remove(listener);
    }
}
