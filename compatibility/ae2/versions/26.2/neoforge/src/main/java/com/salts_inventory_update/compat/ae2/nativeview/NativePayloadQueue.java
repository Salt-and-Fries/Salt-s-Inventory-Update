package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.BiPredicate;

/** Menu-owned native intents. A busy authenticated lane never consumes its head. */
final class NativePayloadQueue<C> {
    private record Pending<C>(C channel, byte[] data, String key) { }
    private final ArrayDeque<Pending<C>> pending = new ArrayDeque<>();
    private final int capacity;

    NativePayloadQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Native payload capacity must be positive");
        this.capacity = capacity;
    }

    boolean add(C channel, byte[] data, String key) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(data, "data");
        // Match the existing authenticated transport limit before allocating a copy.
        if (data.length > 32_768) return false;
        var last = pending.peekLast();
        boolean replace = key != null && last != null && key.equals(last.key()) && channel.equals(last.channel());
        if (!replace && pending.size() >= capacity) throw new IllegalStateException("Native payload queue is full");
        var action = new Pending<>(channel, data.clone(), key);
        if (replace) pending.removeLast();
        pending.addLast(action);
        return true;
    }

    boolean drain(BiPredicate<C, byte[]> send) {
        Objects.requireNonNull(send, "send");
        var head = pending.peekFirst();
        if (head == null || !send.test(head.channel(), head.data().clone())) return false;
        // A synchronous lifetime callback may have cleared or rebound the queue.
        if (pending.peekFirst() == head) pending.removeFirst();
        return true;
    }

    int size() { return pending.size(); }
    boolean isEmpty() { return pending.isEmpty(); }
    void clear() { pending.clear(); }
}
