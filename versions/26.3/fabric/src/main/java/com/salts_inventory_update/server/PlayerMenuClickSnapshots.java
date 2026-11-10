package com.salts_inventory_update.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-thread history of player states published before the cursor is usable. */
final class PlayerMenuClickSnapshots {
    private static final int LIMIT = 32;
    private static final long MAX_AGE_NANOS = 2_000_000_000L;
    private final LinkedHashMap<Integer, Snapshot> snapshots = new LinkedHashMap<>();

    void rememberPublished(net.minecraft.world.inventory.AbstractContainerMenu menu, long now) {
        // Vanilla assigns new revisions while publishing slot/cursor changes.
        // Capture only after that publication, before SIU exposes its cursor.
        menu.broadcastChanges();
        // Modern vanilla cursor-only packets do not always advance stateId.
        // Never attach changed contents to an already immutable publication.
        Snapshot previous;
        while ((previous = snapshots.get(menu.getStateId())) != null
                && !previous.matches(menu.slots, menu.getCarried())) {
            menu.incrementStateId();
        }
        remember(menu.getStateId(), menu.slots, menu.getCarried(), now);
    }

    void remember(int revision, List<Slot> slots, ItemStack carried, long now) {
        snapshots.entrySet().removeIf(entry -> now - entry.getValue().publishedAt > MAX_AGE_NANOS);
        // A revision identifies one immutable published state, even if an
        // upstream item has changed again before its next native broadcast.
        if (snapshots.containsKey(revision)) return;
        List<ItemStack> contents = new ArrayList<>(slots.size());
        for (Slot slot : slots) contents.add(slot.getItem().copy());
        snapshots.put(revision, new Snapshot(List.copyOf(slots), List.copyOf(contents), carried.copy(), now));
        while (snapshots.size() > LIMIT) snapshots.remove(snapshots.keySet().iterator().next());
    }

    boolean permits(int revision, int clicked, List<Slot> slots, ItemStack carried,
                    ItemStack clientCarried, Predicate<Slot> lockedSource, long now) {
        Snapshot snapshot = snapshots.get(revision);
        if (snapshot == null || now - snapshot.publishedAt < 0 || now - snapshot.publishedAt > MAX_AGE_NANOS
                || clicked < 0 || clicked >= slots.size() || slots.size() != snapshot.slots.size()
                || !ItemStack.matches(snapshot.carried, carried)
                || !ItemStack.matches(snapshot.carried, clientCarried)) return false;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot != snapshot.slots.get(i)) return false;
            ItemStack before = snapshot.contents.get(i), current = slot.getItem();
            if (ItemStack.matches(before, current)) continue;
            // Only a different, currently locked source may update metadata.
            // The clicked item, source identity/count, every editable item and
            // the complete cursor must still match the client's published view.
            if (i == clicked || !lockedSource.test(slot) || before.isEmpty() || current.isEmpty()
                    || before.getItem() != current.getItem() || before.getCount() != current.getCount()) return false;
        }
        return true;
    }

    void clear() { snapshots.clear(); }

    private record Snapshot(List<Slot> slots, List<ItemStack> contents, ItemStack carried, long publishedAt) {
        boolean matches(List<Slot> current, ItemStack cursor) {
            if (current.size() != slots.size() || !ItemStack.matches(carried, cursor)) return false;
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i) != slots.get(i) || !ItemStack.matches(contents.get(i), current.get(i).getItem())) return false;
            }
            return true;
        }
    }
}
