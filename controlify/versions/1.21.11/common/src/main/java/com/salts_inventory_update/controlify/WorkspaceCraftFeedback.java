package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.controlify.input.WorkspaceContainerScreen;
import com.salts_inventory_update.controlify.model.ObservedCraftFeedback;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Read-only observation through the published workspace API; never owns a menu or transaction. */
final class WorkspaceCraftFeedback {
    record Attempt(ObservedCraftFeedback.Identity identity, ItemStack output, long baseline,
                   Map<Container, Set<Integer>> physicalSlots) { }
    private record Snapshot(Map<Container, Set<Integer>> physicalSlots, long count) { }
    private final ObservedCraftFeedback observer = new ObservedCraftFeedback();
    private ObservedCraftFeedback.Identity identity;
    private ItemStack output = ItemStack.EMPTY;
    private Map<Container, Set<Integer>> physicalSlots = Map.of();

    Attempt before(DesktopWorkspace desktop, UUID source, UUID destination, ItemStack result) {
        if (result.isEmpty()) return null;
        var snapshot = snapshot(desktop, destination, result);
        return snapshot == null ? null : new Attempt(new ObservedCraftFeedback.Identity("CRAFT_COMPLETE", source, destination),
            result.copy(), snapshot.count(), snapshot.physicalSlots());
    }

    void accepted(Attempt attempt) {
        if (attempt != null && observer.arm(attempt.identity(), attempt.baseline(), Math.max(1, attempt.output().getCount()), millis())) {
            identity = attempt.identity(); output = attempt.output().copy(); physicalSlots = attempt.physicalSlots();
        }
    }

    void tick(DesktopWorkspace desktop) {
        if (!(Minecraft.getInstance().screen instanceof WorkspaceContainerScreen)) { reset(); return; }
        if (!observer.pending() || identity == null) return;
        boolean valid = desktop != null && desktop.isValid()
            && desktop.windows().stream().anyMatch(w -> w.handle().instanceId().equals(identity.sourceId()) && w.handle().isOpen())
            && desktop.windows().stream().anyMatch(w -> w.handle().instanceId().equals(identity.destinationId()) && w.handle().isOpen());
        var current = valid ? snapshot(desktop, identity.destinationId(), output) : null;
        if (current != null && !samePhysicalSlots(physicalSlots, current.physicalSlots())) valid = false;
        var count = current == null ? OptionalLong.empty() : OptionalLong.of(current.count());
        if (observer.update(identity, count, valid, millis()) == ObservedCraftFeedback.Result.COMPLETE)
            WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CRAFT_COMPLETE);
        if (!observer.pending()) reset();
    }

    void reset() { observer.reset(); identity = null; output = ItemStack.EMPTY; physicalSlots = Map.of(); }

    private static Snapshot snapshot(DesktopWorkspace desktop, UUID destination, ItemStack output) {
        Map<Container, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        int uniqueSlots = 0;
        for (var hit : desktop.visibleSlots(destination)) {
            if (hit.menuSlotId() < 0) continue;
            var slot = desktop.hoveredSlot(destination, hit.x() + 8, hit.y() + 8);
            // Derived recipe results and furnace outputs cannot receive the routed craft. Never
            // treat their preview or autonomous production as a destination increase.
            if (slot == null || !slot.isActive() || slot.isFake() || !slot.mayPlace(output)) continue;
            int size = slot.container.getContainerSize(), index = slot.getContainerSlot();
            if (size < 0 || size > 4096) return null;
            if (index < 0 || index >= size) continue;
            if (physicalSlots.computeIfAbsent(slot.container, ignored -> new HashSet<>()).add(index)
                && ++uniqueSlots > 4096) return null;
        }
        if (physicalSlots.isEmpty()) return null;
        long count = 0;
        for (var entry : physicalSlots.entrySet()) {
            for (int index : entry.getValue()) {
                var stack = entry.getKey().getItem(index);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, output)) count += stack.getCount();
            }
            entry.setValue(Set.copyOf(entry.getValue()));
        }
        return new Snapshot(Collections.unmodifiableMap(physicalSlots), count);
    }

    private static boolean samePhysicalSlots(Map<Container, Set<Integer>> expected, Map<Container, Set<Integer>> actual) {
        if (expected.size() != actual.size()) return false;
        for (var entry : expected.entrySet()) if (!entry.getValue().equals(actual.get(entry.getKey()))) return false;
        return true;
    }

    private static long millis() { return System.nanoTime() / 1_000_000L; }
}
