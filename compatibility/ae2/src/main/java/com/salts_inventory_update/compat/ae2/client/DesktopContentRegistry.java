package com.salts_inventory_update.compat.ae2.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Lifetime-addressed native AE2 packet receivers. Adapted from the desktop
 * integration in Salt-and-Fries/Applied-Energistics-2-Salts-inventory-api-test.
 * The packet type is supplied by the loader adapter; AE2 itself is never bundled.
 * All operations run on the client thread.
 */
public final class DesktopContentRegistry<P> {
    private static final int MAX_PENDING_OWNERS = 16;
    private static final int MAX_PENDING_PACKETS = 64;
    private static final int MAX_PENDING_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CLOSED_OWNERS = 64;

    private final Map<UUID, Consumer<P>> receivers = new HashMap<>();
    private final Map<UUID, ArrayDeque<PendingUpdate<P>>> pending = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> closed = new LinkedHashSet<>();
    private final Consumer<P> discard;

    public DesktopContentRegistry() { this(packet -> { }); }

    /** The receiver owns delivered packets; this callback owns packets discarded before delivery. */
    public DesktopContentRegistry(Consumer<P> discard) { this.discard = Objects.requireNonNull(discard, "discard"); }

    private record PendingUpdate<P>(P packet, int bytes) { }

    public void register(UUID owner, Consumer<P> receiver) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(receiver, "receiver");
        if (receivers.putIfAbsent(owner, receiver) != null) {
            throw new IllegalStateException("Duplicate desktop menu identity");
        }
        var queued = pending.remove(owner);
        if (queued != null) {
            try {
                while (!queued.isEmpty()) receiver.accept(queued.removeFirst().packet());
            } finally {
                discard(queued);
            }
        }
    }

    public void remove(UUID owner) {
        receivers.remove(owner);
        discard(pending.remove(owner));
        closed.add(owner);
        if (closed.size() > MAX_CLOSED_OWNERS) {
            closed.remove(closed.iterator().next());
        }
    }

    public boolean receive(UUID owner, P packet) {
        var receiver = receivers.get(owner);
        if (receiver == null) {
            return false;
        }
        receiver.accept(packet);
        return true;
    }

    /** Preserve bounded updates arriving before the opening snapshot creates the window. */
    public void awaitOpening(UUID owner, P packet, int encodedBytes) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(packet, "packet");
        if (encodedBytes < 0) {
            throw new IllegalArgumentException("Negative encoded packet length");
        }
        if (closed.contains(owner)) {
            discard.accept(packet);
            return;
        }
        if (!pending.containsKey(owner) && pending.size() >= MAX_PENDING_OWNERS) {
            discard(pending.remove(pending.keySet().iterator().next()));
        }
        var queue = pending.computeIfAbsent(owner, ignored -> new ArrayDeque<>());
        long bytes = queue.stream().mapToLong(PendingUpdate::bytes).sum();
        if (queue.size() >= MAX_PENDING_PACKETS || bytes + encodedBytes > MAX_PENDING_BYTES) {
            discard.accept(packet);
            throw new IllegalStateException("AE desktop updates arrived before a window could be initialized");
        }
        queue.addLast(new PendingUpdate<>(packet, encodedBytes));
    }

    /** Release old connection lifetimes on disconnect or resource disposal. */
    public void clear() {
        receivers.clear();
        pending.values().forEach(this::discard);
        pending.clear();
        closed.clear();
    }

    private void discard(ArrayDeque<PendingUpdate<P>> queue) {
        if (queue != null) while (!queue.isEmpty()) discard.accept(queue.removeFirst().packet());
    }
}
