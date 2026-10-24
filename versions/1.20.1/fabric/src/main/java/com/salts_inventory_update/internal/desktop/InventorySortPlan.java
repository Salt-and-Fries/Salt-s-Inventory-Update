package com.salts_inventory_update.internal.desktop;

import java.util.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Plans only. No inventory is changed until every original stack has a legal destination. */
public final class InventorySortPlan {
    private InventorySortPlan() {}

    public static Optional<List<ItemStack>> create(List<Slot> slots, Comparator<ItemStack> order) {
        List<ItemStack> groups = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            int group = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (ItemStack.isSameItemSameTags(stack, groups.get(i))) { group = i; break; }
            }
            if (group < 0) { groups.add(stack.copyWithCount(1)); counts.add((long) stack.getCount()); }
            else counts.set(group, counts.get(group) + stack.getCount());
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) indices.add(i);
        indices.sort((a, b) -> order.compare(groups.get(a), groups.get(b)));
        List<ItemStack> result = new ArrayList<>(Collections.nCopies(slots.size(), ItemStack.EMPTY));
        // Find legal destinations in row-major order. Restricted slots are never forced to accept an item.
        for (int index : indices) {
            ItemStack stack = groups.get(index);
            long remaining = counts.get(index);
            for (int i = 0; i < slots.size() && remaining > 0; i++) {
                Slot slot = slots.get(i);
                if (!result.get(i).isEmpty() || !slot.mayPlace(stack)) continue;
                int count = (int) Math.min(remaining, Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack)));
                if (count <= 0) continue;
                result.set(i, stack.copyWithCount(count));
                remaining -= count;
            }
            if (remaining != 0) return Optional.empty();
        }
        return Optional.of(List.copyOf(result));
    }
}
